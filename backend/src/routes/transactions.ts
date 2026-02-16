import { Router, Response } from "express";
import { FieldValue } from "firebase-admin/firestore";
import { AuthRequest } from "../middleware/auth";
import { getDb } from "../services/firestore";
import { createNotification } from "../services/notifications";

export const transactionsRouter = Router();

// Create a transaction (offer or request)
transactionsRouter.post("/", async (req: AuthRequest, res: Response) => {
  const { type, targetEmail, targetName, amount, description } = req.body;

  if (!type || !targetEmail || !targetName || !amount || !description) {
    res.status(400).json({ error: "Missing required fields" });
    return;
  }
  if (type !== "offer" && type !== "request") {
    res.status(400).json({ error: "Type must be 'offer' or 'request'" });
    return;
  }
  if (targetEmail === req.userEmail) {
    res.status(400).json({ error: "Cannot create transaction with yourself" });
    return;
  }

  const db = getDb();
  const now = FieldValue.serverTimestamp();
  const docRef = await db.collection("transactions").add({
    type,
    status: "pending",
    initiatorEmail: req.userEmail,
    initiatorName: req.userName,
    targetEmail,
    targetName,
    currentAmount: amount,
    description,
    events: [
      {
        action: "created",
        actorEmail: req.userEmail,
        amount,
        timestamp: new Date(),
      },
    ],
    initiatorCounters: 0,
    targetCounters: 0,
    createdAt: now,
    updatedAt: now,
  });

  const notifType = type === "offer" ? "new_offer" : "new_request";
  const verb = type === "offer" ? "offered" : "requested";
  await createNotification({
    recipientEmail: targetEmail,
    transactionId: docRef.id,
    type: notifType,
    message: `${req.userName} ${verb} ${amount} pardon(s): "${description}"`,
  });

  res.status(201).json({ id: docRef.id });
});

// List transactions for current user
transactionsRouter.get("/", async (req: AuthRequest, res: Response) => {
  const db = getDb();
  const email = req.userEmail!;
  const statusFilter = req.query.status as string | undefined;

  // Query both as initiator and target, then merge
  const [asInitiator, asTarget] = await Promise.all([
    db
      .collection("transactions")
      .where("initiatorEmail", "==", email)
      .get(),
    db
      .collection("transactions")
      .where("targetEmail", "==", email)
      .get(),
  ]);

  const seen = new Set<string>();
  const transactions: any[] = [];

  for (const doc of [...asInitiator.docs, ...asTarget.docs]) {
    if (seen.has(doc.id)) continue;
    seen.add(doc.id);
    const data = doc.data();
    if (statusFilter && data.status !== statusFilter) continue;
    transactions.push({ id: doc.id, ...data });
  }

  transactions.sort((a, b) => {
    const aTime = a.createdAt?.toMillis?.() || 0;
    const bTime = b.createdAt?.toMillis?.() || 0;
    return bTime - aTime;
  });

  res.json(transactions);
});

// Get single transaction
transactionsRouter.get("/:id", async (req: AuthRequest, res: Response) => {
  const db = getDb();
  const doc = await db.collection("transactions").doc(req.params.id).get();
  if (!doc.exists) {
    res.status(404).json({ error: "Transaction not found" });
    return;
  }
  const data = doc.data()!;
  if (data.initiatorEmail !== req.userEmail && data.targetEmail !== req.userEmail) {
    res.status(403).json({ error: "Not a participant" });
    return;
  }
  res.json({ id: doc.id, ...data });
});

// Helper: check if it's the user's turn
export function isUsersTurn(transaction: any, userEmail: string): boolean {
  const events = transaction.events;
  if (!events || events.length === 0) return false;
  const lastActor = events[events.length - 1].actorEmail;
  return lastActor !== userEmail;
}

// Accept
transactionsRouter.post("/:id/accept", async (req: AuthRequest, res: Response) => {
  const db = getDb();
  const docRef = db.collection("transactions").doc(req.params.id);
  const doc = await docRef.get();
  if (!doc.exists) {
    res.status(404).json({ error: "Transaction not found" });
    return;
  }

  const data = doc.data()!;
  if (data.status !== "pending") {
    res.status(400).json({ error: "Transaction is not pending" });
    return;
  }
  if (data.initiatorEmail !== req.userEmail && data.targetEmail !== req.userEmail) {
    res.status(403).json({ error: "Not a participant" });
    return;
  }
  if (!isUsersTurn(data, req.userEmail!)) {
    res.status(400).json({ error: "Not your turn" });
    return;
  }

  await docRef.update({
    status: "accepted",
    events: FieldValue.arrayUnion({
      action: "accepted",
      actorEmail: req.userEmail,
      amount: data.currentAmount,
      timestamp: new Date(),
    }),
    updatedAt: FieldValue.serverTimestamp(),
  });

  const otherEmail =
    data.initiatorEmail === req.userEmail ? data.targetEmail : data.initiatorEmail;
  await createNotification({
    recipientEmail: otherEmail,
    transactionId: doc.id,
    type: "accepted",
    message: `${req.userName} accepted the pardon of ${data.currentAmount}: "${data.description}"`,
  });

  res.json({ success: true });
});

// Reject
transactionsRouter.post("/:id/reject", async (req: AuthRequest, res: Response) => {
  const db = getDb();
  const docRef = db.collection("transactions").doc(req.params.id);
  const doc = await docRef.get();
  if (!doc.exists) {
    res.status(404).json({ error: "Transaction not found" });
    return;
  }

  const data = doc.data()!;
  if (data.status !== "pending") {
    res.status(400).json({ error: "Transaction is not pending" });
    return;
  }
  if (data.initiatorEmail !== req.userEmail && data.targetEmail !== req.userEmail) {
    res.status(403).json({ error: "Not a participant" });
    return;
  }
  if (!isUsersTurn(data, req.userEmail!)) {
    res.status(400).json({ error: "Not your turn" });
    return;
  }

  await docRef.update({
    status: "rejected",
    events: FieldValue.arrayUnion({
      action: "rejected",
      actorEmail: req.userEmail,
      amount: data.currentAmount,
      message: req.body.message || undefined,
      timestamp: new Date(),
    }),
    updatedAt: FieldValue.serverTimestamp(),
  });

  const otherEmail =
    data.initiatorEmail === req.userEmail ? data.targetEmail : data.initiatorEmail;
  await createNotification({
    recipientEmail: otherEmail,
    transactionId: doc.id,
    type: "rejected",
    message: `${req.userName} rejected the pardon: "${data.description}"`,
  });

  res.json({ success: true });
});

// Counter-offer
transactionsRouter.post("/:id/counter", async (req: AuthRequest, res: Response) => {
  const { amount, message } = req.body;
  if (!amount || amount < 1) {
    res.status(400).json({ error: "Amount must be at least 1" });
    return;
  }

  const db = getDb();
  const docRef = db.collection("transactions").doc(req.params.id);
  const doc = await docRef.get();
  if (!doc.exists) {
    res.status(404).json({ error: "Transaction not found" });
    return;
  }

  const data = doc.data()!;
  if (data.status !== "pending") {
    res.status(400).json({ error: "Transaction is not pending" });
    return;
  }
  if (data.initiatorEmail !== req.userEmail && data.targetEmail !== req.userEmail) {
    res.status(403).json({ error: "Not a participant" });
    return;
  }
  if (!isUsersTurn(data, req.userEmail!)) {
    res.status(400).json({ error: "Not your turn" });
    return;
  }

  const isInitiator = data.initiatorEmail === req.userEmail;
  const counterField = isInitiator ? "initiatorCounters" : "targetCounters";
  const currentCounters = data[counterField] || 0;

  if (currentCounters >= 2) {
    res.status(400).json({ error: "Maximum counter-offers reached (2)" });
    return;
  }

  const event: any = {
    action: "countered",
    actorEmail: req.userEmail,
    amount,
    timestamp: new Date(),
  };
  if (message) event.message = message;

  await docRef.update({
    currentAmount: amount,
    [counterField]: currentCounters + 1,
    events: FieldValue.arrayUnion(event),
    updatedAt: FieldValue.serverTimestamp(),
  });

  const otherEmail = isInitiator ? data.targetEmail : data.initiatorEmail;
  await createNotification({
    recipientEmail: otherEmail,
    transactionId: doc.id,
    type: "countered",
    message: `${req.userName} counter-offered ${amount} pardon(s) on "${data.description}"`,
  });

  res.json({ success: true });
});
