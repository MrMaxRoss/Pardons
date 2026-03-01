import { Router, Response } from "express";
import { AuthRequest } from "../middleware/auth";
import { getDb } from "../services/firestore";

export const notificationsRouter = Router();

// Get unread notifications for current user
notificationsRouter.get("/", async (req: AuthRequest, res: Response) => {
  const db = getDb();
  const snapshot = await db
    .collection("notifications")
    .where("recipientEmail", "==", req.userEmail)
    .where("read", "==", false)
    .orderBy("createdAt", "desc")
    .limit(50)
    .get();

  const notifications = snapshot.docs.map((doc) => ({
    id: doc.id,
    ...doc.data(),
  }));
  res.json(notifications);
});

// Mark notification as read
notificationsRouter.post("/:id/read", async (req: AuthRequest, res: Response) => {
  const db = getDb();
  const docRef = db.collection("notifications").doc(req.params.id);
  const doc = await docRef.get();

  if (!doc.exists) {
    res.status(404).json({ error: "Notification not found" });
    return;
  }
  if (doc.data()!.recipientEmail !== req.userEmail) {
    res.status(403).json({ error: "Not your notification" });
    return;
  }

  await docRef.update({ read: true });
  res.json({ success: true });
});

// Mark all notifications as read
notificationsRouter.post("/read-all", async (req: AuthRequest, res: Response) => {
  const db = getDb();
  const snapshot = await db
    .collection("notifications")
    .where("recipientEmail", "==", req.userEmail)
    .where("read", "==", false)
    .get();

  const batch = db.batch();
  for (const doc of snapshot.docs) {
    batch.update(doc.ref, { read: true });
  }
  await batch.commit();

  res.json({ success: true, cleared: snapshot.size });
});

// Mark all notifications for a transaction as read
notificationsRouter.post("/read-by-transaction/:transactionId", async (req: AuthRequest, res: Response) => {
  const db = getDb();
  const snapshot = await db
    .collection("notifications")
    .where("recipientEmail", "==", req.userEmail)
    .where("transactionId", "==", req.params.transactionId)
    .where("read", "==", false)
    .get();

  const batch = db.batch();
  for (const doc of snapshot.docs) {
    batch.update(doc.ref, { read: true });
  }
  await batch.commit();

  res.json({ success: true, cleared: snapshot.size });
});
