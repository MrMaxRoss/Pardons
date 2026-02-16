import sgMail from "@sendgrid/mail";

if (process.env.SENDGRID_API_KEY) {
  sgMail.setApiKey(process.env.SENDGRID_API_KEY);
}

const FROM_EMAIL = "pardons@rossFamily.app";

export async function sendEmail(
  to: string,
  subject: string,
  text: string
): Promise<void> {
  if (!process.env.SENDGRID_API_KEY) {
    console.log(`[Email skipped - no API key] To: ${to}, Subject: ${subject}`);
    return;
  }

  try {
    await sgMail.send({ to, from: FROM_EMAIL, subject, text });
  } catch (err) {
    console.error("Failed to send email:", err);
  }
}
