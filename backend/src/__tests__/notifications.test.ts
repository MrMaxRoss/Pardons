import { describe, it, expect, vi, beforeEach } from "vitest";
import request from "supertest";

// --- Mocks ---

const mockGet = vi.fn();
const mockUpdate = vi.fn();
const mockWhere = vi.fn();
const mockOrderBy = vi.fn();
const mockLimit = vi.fn();
const mockDoc = vi.fn();
const mockCollection = vi.fn();
const mockBatchUpdate = vi.fn();
const mockBatchCommit = vi.fn();

function resetChainableMock() {
  const chain: any = {
    where: mockWhere,
    orderBy: mockOrderBy,
    limit: mockLimit,
    get: mockGet,
    doc: mockDoc,
  };
  mockCollection.mockReturnValue(chain);
  mockWhere.mockReturnValue(chain);
  mockOrderBy.mockReturnValue(chain);
  mockLimit.mockReturnValue(chain);
  mockDoc.mockReturnValue({ get: mockGet, update: mockUpdate });
}

vi.mock("../services/firestore", () => ({
  getDb: () => ({
    collection: mockCollection,
    batch: () => ({
      update: mockBatchUpdate,
      commit: mockBatchCommit,
    }),
  }),
  initializeFirestore: vi.fn(),
}));

vi.mock("../services/notifications", () => ({
  createNotification: vi.fn().mockResolvedValue(undefined),
}));

vi.mock("firebase-admin/firestore", () => ({
  FieldValue: {
    serverTimestamp: () => "SERVER_TIMESTAMP",
    arrayUnion: (...args: any[]) => ({ _arrayUnion: args }),
  },
}));

vi.mock("google-auth-library", () => ({
  OAuth2Client: vi.fn().mockImplementation(() => ({
    verifyIdToken: vi.fn(),
  })),
}));

import { app } from "../index";

const agent = request(app);
const AUTH = "Bearer dev:max.ross@gmail.com";

describe("GET /api/notifications", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    resetChainableMock();
  });

  it("returns unread notifications for the user", async () => {
    const notif1 = {
      id: "n1",
      data: () => ({
        recipientEmail: "max.ross@gmail.com",
        message: "New offer",
        read: false,
      }),
    };
    mockGet.mockResolvedValueOnce({ docs: [notif1] });

    const res = await agent
      .get("/api/notifications")
      .set("Authorization", AUTH);

    expect(res.status).toBe(200);
    expect(res.body).toHaveLength(1);
    expect(res.body[0].id).toBe("n1");
    expect(res.body[0].message).toBe("New offer");
  });
});

describe("POST /api/notifications/:id/read", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    resetChainableMock();
  });

  it("returns 404 when notification not found", async () => {
    mockGet.mockResolvedValueOnce({ exists: false });

    const res = await agent
      .post("/api/notifications/nonexistent/read")
      .set("Authorization", AUTH);

    expect(res.status).toBe(404);
  });

  it("returns 403 when notification belongs to another user", async () => {
    mockGet.mockResolvedValueOnce({
      exists: true,
      data: () => ({ recipientEmail: "daphne.ross@gmail.com" }),
    });

    const res = await agent
      .post("/api/notifications/n1/read")
      .set("Authorization", AUTH);

    expect(res.status).toBe(403);
  });

  it("marks notification as read", async () => {
    mockGet.mockResolvedValueOnce({
      exists: true,
      data: () => ({ recipientEmail: "max.ross@gmail.com" }),
    });
    mockUpdate.mockResolvedValueOnce(undefined);

    const res = await agent
      .post("/api/notifications/n1/read")
      .set("Authorization", AUTH);

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(mockUpdate).toHaveBeenCalledWith({ read: true });
  });
});

describe("POST /api/notifications/read-by-transaction/:transactionId", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    resetChainableMock();
  });

  it("batch marks all matching notifications as read", async () => {
    const doc1 = { ref: { id: "n1" } };
    const doc2 = { ref: { id: "n2" } };
    mockGet.mockResolvedValueOnce({ docs: [doc1, doc2], size: 2 });
    mockBatchCommit.mockResolvedValueOnce(undefined);

    const res = await agent
      .post("/api/notifications/read-by-transaction/txn-1")
      .set("Authorization", AUTH);

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.cleared).toBe(2);
    expect(mockBatchUpdate).toHaveBeenCalledTimes(2);
  });
});
