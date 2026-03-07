import sgMail from "@sendgrid/mail";

if (process.env.SENDGRID_API_KEY) {
  sgMail.setApiKey(process.env.SENDGRID_API_KEY);
}

const FROM_EMAIL = "max@sortedunderbelly.com";
const EMAIL_OVERRIDE = process.env.EMAIL_OVERRIDE ?? "max.ross@gmail.com";

export async function sendEmail(
  to: string,
  subject: string,
  text: string,
  html?: string
): Promise<void> {
  if (!process.env.SENDGRID_API_KEY) {
    console.log(`[Email skipped - no API key] To: ${to}, Subject: ${subject}`);
    return;
  }

  const recipient = EMAIL_OVERRIDE || to;

  try {
    await sgMail.send({
      to: recipient,
      from: FROM_EMAIL,
      subject: EMAIL_OVERRIDE ? `[To: ${to}] ${subject}` : subject,
      text,
      ...(html && { html }),
    });
  } catch (err) {
    console.error("Failed to send email:", err);
  }
}
