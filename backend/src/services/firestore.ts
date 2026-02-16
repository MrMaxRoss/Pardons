import { initializeApp, cert, applicationDefault, getApps } from "firebase-admin/app";
import { getFirestore, Firestore } from "firebase-admin/firestore";

let db: Firestore;

export function initializeFirestore(): Firestore {
  if (!getApps().length) {
    if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
      initializeApp({ credential: cert(process.env.GOOGLE_APPLICATION_CREDENTIALS) });
    } else {
      initializeApp({ projectId: process.env.GCLOUD_PROJECT || "pardons-local" });
    }
  }
  db = getFirestore();

  if (process.env.FIRESTORE_EMULATOR_HOST) {
    console.log(`Using Firestore emulator at ${process.env.FIRESTORE_EMULATOR_HOST}`);
  }

  return db;
}

export function getDb(): Firestore {
  if (!db) {
    throw new Error("Firestore not initialized");
  }
  return db;
}
