import { Request, Response, NextFunction } from "express";
import { OAuth2Client } from "google-auth-library";

const ALLOWED_EMAILS: Record<string, string> = {
  "max.ross@gmail.com": "Max",
  "daphne.ross@gmail.com": "Daphne",
  "violet.ross@gmail.com": "Violet",
};

const DEV_AUTH = process.env.DEV_AUTH === "true";
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

  // Dev mode: token is "dev:<email>"
  if (DEV_AUTH && token.startsWith("dev:")) {
    const email = token.slice(4);
    if (!(email in ALLOWED_EMAILS)) {
      res.status(403).json({ error: "Unauthorized user" });
      return;
    }
    req.userEmail = email;
    req.userName = ALLOWED_EMAILS[email];
    next();
    return;
  }

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

    if (!(payload.email in ALLOWED_EMAILS)) {
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
