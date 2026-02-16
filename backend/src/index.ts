import express from "express";
import cors from "cors";
import { initializeFirestore } from "./services/firestore";
import { authMiddleware } from "./middleware/auth";
import { transactionsRouter } from "./routes/transactions";
import { usersRouter } from "./routes/users";
import { notificationsRouter } from "./routes/notifications";

const app = express();
const PORT = parseInt(process.env.PORT || "8080", 10);

app.use(cors({
  origin: process.env.CORS_ORIGIN || "http://localhost:5173",
  credentials: true,
}));
app.use(express.json());

app.get("/api/health", (_req, res) => {
  res.json({ status: "ok" });
});

app.use("/api", authMiddleware);
app.use("/api/transactions", transactionsRouter);
app.use("/api/users", usersRouter);
app.use("/api/notifications", notificationsRouter);

initializeFirestore();

app.listen(PORT, () => {
  console.log(`Pardons backend listening on port ${PORT}`);
});
