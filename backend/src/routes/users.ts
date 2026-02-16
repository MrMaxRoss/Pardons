import { Router, Response } from "express";
import { FieldValue } from "firebase-admin/firestore";
import { AuthRequest } from "../middleware/auth";
import { getDb } from "../services/firestore";

export const usersRouter = Router();

// Upsert current user on login
usersRouter.post("/me", async (req: AuthRequest, res: Response) => {
  const db = getDb();
  const email = req.userEmail!;
  await db.collection("users").doc(email).set(
    {
      email,
      displayName: req.userName || email,
      photoUrl: req.body.photoUrl || null,
      lastLogin: FieldValue.serverTimestamp(),
    },
    { merge: true }
  );
  res.json({ success: true });
});

// List all family members
usersRouter.get("/", async (_req: AuthRequest, res: Response) => {
  const db = getDb();
  const snapshot = await db.collection("users").get();
  const users = snapshot.docs.map((doc) => ({ email: doc.id, ...doc.data() }));
  res.json(users);
});
