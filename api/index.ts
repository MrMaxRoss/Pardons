import { initializeFirestore } from "../backend/src/services/firestore";
import { app } from "../backend/src/index";

initializeFirestore();

export default app;
