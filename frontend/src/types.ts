export interface TransactionEvent {
  action: "created" | "accepted" | "rejected" | "countered";
  actorEmail: string;
  amount: number;
  message?: string;
  timestamp: any;
}

export interface Transaction {
  id: string;
  type: "offer" | "request";
  status: "pending" | "accepted" | "rejected";
  initiatorEmail: string;
  initiatorName: string;
  targetEmail: string;
  targetName: string;
  currentAmount: number;
  description: string;
  events: TransactionEvent[];
  initiatorCounters: number;
  targetCounters: number;
  createdAt: any;
  updatedAt: any;
}

export interface Notification {
  id: string;
  recipientEmail: string;
  transactionId: string;
  type: "new_offer" | "new_request" | "countered" | "accepted" | "rejected";
  message: string;
  read: boolean;
  createdAt: any;
}

export interface User {
  email: string;
  displayName: string;
  photoUrl?: string;
}
