import { initializeApp, cert, getApps } from "firebase-admin/app";
import { getFirestore, Firestore } from "firebase-admin/firestore";

let db: Firestore;

export function initializeFirestore(): Firestore {
  if (!getApps().length) {
    initializeApp({
      credential: process.env.GOOGLE_APPLICATION_CREDENTIALS
        ? cert(process.env.GOOGLE_APPLICATION_CREDENTIALS)
        : undefined,
    });
  }
  db = getFirestore();
  return db;
}

export function getDb(): Firestore {
  if (!db) {
    throw new Error("Firestore not initialized");
  }
  return db;
}
