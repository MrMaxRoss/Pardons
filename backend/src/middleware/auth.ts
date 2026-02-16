import { Request, Response, NextFunction } from "express";
import { OAuth2Client } from "google-auth-library";

const ALLOWED_EMAILS = [
  "max.ross@gmail.com",
  "daphne.ross@gmail.com",
  "violet.ross@gmail.com",
];

const client = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

export interface AuthRequest extends Request {
  userEmail?: string;
  userName?: string;
}

export async function authMiddleware(
  req: AuthRequest,
  res: Response,
  next: NextFunction
) {
  const authHeader = req.headers.authorization;
  if (!authHeader?.startsWith("Bearer ")) {
    res.status(401).json({ error: "Missing authorization token" });
    return;
  }

  const token = authHeader.slice(7);

  try {
    const ticket = await client.verifyIdToken({
      idToken: token,
      audience: process.env.GOOGLE_CLIENT_ID,
    });
    const payload = ticket.getPayload();
    if (!payload?.email) {
      res.status(401).json({ error: "Invalid token" });
      return;
    }

    if (!ALLOWED_EMAILS.includes(payload.email)) {
      res.status(403).json({ error: "Unauthorized user" });
      return;
    }

    req.userEmail = payload.email;
    req.userName = payload.name || payload.email;
    next();
  } catch {
    res.status(401).json({ error: "Invalid or expired token" });
  }
}
