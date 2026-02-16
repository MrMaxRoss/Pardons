import { getDb } from "./firestore";
import { FieldValue } from "firebase-admin/firestore";
import { sendEmail } from "./email";

type NotificationType =
  | "new_offer"
  | "new_request"
  | "countered"
  | "accepted"
  | "rejected";

export async function createNotification(params: {
  recipientEmail: string;
  transactionId: string;
  type: NotificationType;
  message: string;
}): Promise<void> {
  const db = getDb();
  await db.collection("notifications").add({
    recipientEmail: params.recipientEmail,
    transactionId: params.transactionId,
    type: params.type,
    message: params.message,
    read: false,
    createdAt: FieldValue.serverTimestamp(),
  });

  await sendEmail(
    params.recipientEmail,
    `Pardons: ${params.type.replace("_", " ")}`,
    params.message
  );
}
