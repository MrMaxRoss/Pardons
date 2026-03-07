import { Router, Response } from "express";
import { FieldValue } from "firebase-admin/firestore";
import { AuthRequest } from "../middleware/auth";
import { getDb } from "../services/firestore";
import { createNotification } from "../services/notifications";

export const transactionsRouter = Router();

function formatDate(ts: any): string {
  if (!ts) return "an earlier date";
  const date = ts.toDate ? ts.toDate() : ts._seconds ? new Date(ts._seconds * 1000) : new Date(ts);
  if (isNaN(date.getTime())) return "an earlier date";
  return date.toLocaleDateString();
}

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
  const notifMessage = type === "offer"
    ? `${req.userName} is offering you ${amount} pardon(s) for "${description}".`
    : `${req.userName} is requesting ${amount} pardon(s) from you for "${description}".`;
  await createNotification({
    recipientEmail: targetEmail,
    transactionId: docRef.id,
    type: notifType,
    message: notifMessage,
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
  const dateStr = formatDate(data.events[0]?.timestamp);
  // The recipient of this email is otherEmail (the person who did NOT act), so "you" = otherEmail
  const recipientIsInitiator = data.initiatorEmail !== req.userEmail;
  const acceptMessage = recipientIsInitiator
    ? data.type === "offer"
      ? `On ${dateStr}, you offered ${data.targetName} ${data.currentAmount} pardon(s) for "${data.description}". ${req.userName} has accepted.`
      : `On ${dateStr}, you requested ${data.currentAmount} pardon(s) from ${data.targetName} for "${data.description}". ${req.userName} has accepted.`
    : data.type === "offer"
      ? `On ${dateStr}, ${data.initiatorName} offered you ${data.currentAmount} pardon(s) for "${data.description}". ${req.userName} has accepted.`
      : `On ${dateStr}, ${data.initiatorName} requested ${data.currentAmount} pardon(s) from you for "${data.description}". ${req.userName} has accepted.`;
  await createNotification({
    recipientEmail: otherEmail,
    transactionId: doc.id,
    type: "accepted",
    message: acceptMessage,
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

  const rejectEvent: any = {
    action: "rejected",
    actorEmail: req.userEmail,
    amount: data.currentAmount,
    timestamp: new Date(),
  };
  if (req.body.message) rejectEvent.message = req.body.message;

  await docRef.update({
    status: "rejected",
    events: FieldValue.arrayUnion(rejectEvent),
    updatedAt: FieldValue.serverTimestamp(),
  });

  const otherEmail =
    data.initiatorEmail === req.userEmail ? data.targetEmail : data.initiatorEmail;
  const rejectDateStr = formatDate(data.events[0]?.timestamp);
  // The recipient of this email is otherEmail (the person who did NOT reject), so "you" = otherEmail
  const rejectRecipientIsInitiator = data.initiatorEmail !== req.userEmail;
  const rejectMessage = rejectRecipientIsInitiator
    ? data.type === "offer"
      ? `On ${rejectDateStr}, you offered ${data.targetName} ${data.currentAmount} pardon(s) for "${data.description}". Unfortunately, ${req.userName} rejected the offer.`
      : `On ${rejectDateStr}, you requested ${data.currentAmount} pardon(s) from ${data.targetName} for "${data.description}". Unfortunately, ${req.userName} rejected the request.`
    : data.type === "offer"
      ? `On ${rejectDateStr}, ${data.initiatorName} offered you ${data.currentAmount} pardon(s) for "${data.description}". Unfortunately, ${req.userName} rejected the offer.`
      : `On ${rejectDateStr}, ${data.initiatorName} requested ${data.currentAmount} pardon(s) from you for "${data.description}". Unfortunately, ${req.userName} rejected the request.`;
  await createNotification({
    recipientEmail: otherEmail,
    transactionId: doc.id,
    type: "rejected",
    message: rejectMessage,
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
  const counterMessage = `${req.userName} has counter-offered ${amount} pardon(s) on "${data.description}"${message ? `: "${message}"` : ""}.`;
  await createNotification({
    recipientEmail: otherEmail,
    transactionId: doc.id,
    type: "countered",
    message: counterMessage,
  });

  res.json({ success: true });
});
