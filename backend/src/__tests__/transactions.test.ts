import { describe, it, expect, vi, beforeEach } from "vitest";
import request from "supertest";

// --- Mocks ---

// Mock firestore
const mockAdd = vi.fn();
const mockGet = vi.fn();
const mockUpdate = vi.fn();
const mockWhere = vi.fn();
const mockDoc = vi.fn();
const mockOrderBy = vi.fn();
const mockCollection = vi.fn();

function resetChainableMock() {
  const chain: any = {
    where: mockWhere,
    orderBy: mockOrderBy,
    get: mockGet,
    doc: mockDoc,
    add: mockAdd,
  };
  mockCollection.mockReturnValue(chain);
  mockWhere.mockReturnValue(chain);
  mockOrderBy.mockReturnValue(chain);
  mockDoc.mockReturnValue({ get: mockGet, update: mockUpdate, id: "txn-1" });
}

vi.mock("../services/firestore", () => ({
  getDb: () => ({ collection: mockCollection }),
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
import { isUsersTurn } from "../routes/transactions";
import { createNotification } from "../services/notifications";

const agent = request(app);
const AUTH = "Bearer dev:max.ross@gmail.com";

describe("isUsersTurn", () => {
  it("returns false for empty events", () => {
    expect(isUsersTurn({ events: [] }, "max.ross@gmail.com")).toBe(false);
  });

  it("returns false when no events array", () => {
    expect(isUsersTurn({}, "max.ross@gmail.com")).toBe(false);
  });

  it("returns false when user was the last actor", () => {
    const txn = {
      events: [{ actorEmail: "max.ross@gmail.com" }],
    };
    expect(isUsersTurn(txn, "max.ross@gmail.com")).toBe(false);
  });

  it("returns true when someone else was the last actor", () => {
    const txn = {
      events: [
        { actorEmail: "max.ross@gmail.com" },
        { actorEmail: "daphne.ross@gmail.com" },
      ],
    };
    expect(isUsersTurn(txn, "max.ross@gmail.com")).toBe(true);
  });
});

describe("POST /api/transactions", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    resetChainableMock();
  });

  it("returns 400 for missing fields", async () => {
    const res = await agent
      .post("/api/transactions")
      .set("Authorization", AUTH)
      .send({ type: "offer" });

    expect(res.status).toBe(400);
    expect(res.body.error).toMatch(/Missing/i);
  });

  it("returns 400 for self-targeting", async () => {
    const res = await agent
      .post("/api/transactions")
      .set("Authorization", AUTH)
      .send({
        type: "offer",
        targetEmail: "max.ross@gmail.com",
        targetName: "Max",
        amount: 3,
        description: "Test",
      });

    expect(res.status).toBe(400);
    expect(res.body.error).toMatch(/yourself/i);
  });

  it("returns 201 for a valid offer and creates notification", async () => {
    mockAdd.mockResolvedValueOnce({ id: "new-txn-id" });

    const res = await agent
      .post("/api/transactions")
      .set("Authorization", AUTH)
      .send({
        type: "offer",
        targetEmail: "daphne.ross@gmail.com",
        targetName: "Daphne",
        amount: 5,
        description: "For doing dishes",
      });

    expect(res.status).toBe(201);
    expect(res.body.id).toBe("new-txn-id");
    expect(createNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        recipientEmail: "daphne.ross@gmail.com",
        type: "new_offer",
      })
    );
  });

  it("returns 201 for a valid request", async () => {
    mockAdd.mockResolvedValueOnce({ id: "req-txn-id" });

    const res = await agent
      .post("/api/transactions")
      .set("Authorization", AUTH)
      .send({
        type: "request",
        targetEmail: "violet.ross@gmail.com",
        targetName: "Violet",
        amount: 2,
        description: "For cleaning room",
      });

    expect(res.status).toBe(201);
    expect(res.body.id).toBe("req-txn-id");
    expect(createNotification).toHaveBeenCalledWith(
      expect.objectContaining({ type: "new_request" })
    );
  });
});

describe("GET /api/transactions", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    resetChainableMock();
  });

  it("returns merged initiator and target transactions", async () => {
    const txn1 = {
      id: "t1",
      data: () => ({
        status: "pending",
        initiatorEmail: "max.ross@gmail.com",
        createdAt: { toMillis: () => 1000 },
      }),
    };
    const txn2 = {
      id: "t2",
      data: () => ({
        status: "pending",
        targetEmail: "max.ross@gmail.com",
        createdAt: { toMillis: () => 2000 },
      }),
    };

    // First call: asInitiator query; second call: asTarget query
    mockGet
      .mockResolvedValueOnce({ docs: [txn1] })
      .mockResolvedValueOnce({ docs: [txn2] });

    const res = await agent
      .get("/api/transactions")
      .set("Authorization", AUTH);

    expect(res.status).toBe(200);
    expect(res.body).toHaveLength(2);
    // Sorted by createdAt desc: t2 first
    expect(res.body[0].id).toBe("t2");
    expect(res.body[1].id).toBe("t1");
  });
});

describe("GET /api/transactions/:id", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    resetChainableMock();
  });

  it("returns 404 when not found", async () => {
    mockGet.mockResolvedValueOnce({ exists: false });

    const res = await agent
      .get("/api/transactions/nonexistent")
      .set("Authorization", AUTH);

    expect(res.status).toBe(404);
  });

  it("returns 403 when user is not a participant", async () => {
    mockGet.mockResolvedValueOnce({
      exists: true,
      id: "txn-1",
      data: () => ({
        initiatorEmail: "daphne.ross@gmail.com",
        targetEmail: "violet.ross@gmail.com",
      }),
    });

    const res = await agent
      .get("/api/transactions/txn-1")
      .set("Authorization", AUTH);

    expect(res.status).toBe(403);
  });

  it("returns 200 with transaction data for a participant", async () => {
    mockGet.mockResolvedValueOnce({
      exists: true,
      id: "txn-1",
      data: () => ({
        initiatorEmail: "max.ross@gmail.com",
        targetEmail: "daphne.ross@gmail.com",
        currentAmount: 5,
      }),
    });

    const res = await agent
      .get("/api/transactions/txn-1")
      .set("Authorization", AUTH);

    expect(res.status).toBe(200);
    expect(res.body.id).toBe("txn-1");
    expect(res.body.currentAmount).toBe(5);
  });
});

describe("POST /api/transactions/:id/accept", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    resetChainableMock();
  });

  it("returns 400 when transaction is not pending", async () => {
    mockGet.mockResolvedValueOnce({
      exists: true,
      id: "txn-1",
      data: () => ({
        status: "accepted",
        initiatorEmail: "max.ross@gmail.com",
        targetEmail: "daphne.ross@gmail.com",
      }),
    });

    const res = await agent
      .post("/api/transactions/txn-1/accept")
      .set("Authorization", AUTH);

    expect(res.status).toBe(400);
    expect(res.body.error).toMatch(/not pending/i);
  });

  it("returns 400 when it is not the user's turn", async () => {
    mockGet.mockResolvedValueOnce({
      exists: true,
      id: "txn-1",
      data: () => ({
        status: "pending",
        initiatorEmail: "max.ross@gmail.com",
        targetEmail: "daphne.ross@gmail.com",
        events: [{ actorEmail: "max.ross@gmail.com" }],
      }),
    });

    const res = await agent
      .post("/api/transactions/txn-1/accept")
      .set("Authorization", AUTH);

    expect(res.status).toBe(400);
    expect(res.body.error).toMatch(/not your turn/i);
  });

  it("accepts and creates notification", async () => {
    mockGet.mockResolvedValueOnce({
      exists: true,
      id: "txn-1",
      data: () => ({
        status: "pending",
        initiatorEmail: "max.ross@gmail.com",
        targetEmail: "daphne.ross@gmail.com",
        currentAmount: 5,
        description: "Dishes",
        events: [{ actorEmail: "daphne.ross@gmail.com" }],
      }),
    });
    mockUpdate.mockResolvedValueOnce(undefined);

    const res = await agent
      .post("/api/transactions/txn-1/accept")
      .set("Authorization", AUTH);

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(mockUpdate).toHaveBeenCalledWith(
      expect.objectContaining({ status: "accepted" })
    );
    expect(createNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        recipientEmail: "daphne.ross@gmail.com",
        type: "accepted",
      })
    );
  });
});

describe("POST /api/transactions/:id/reject", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    resetChainableMock();
  });

  it("rejects and creates notification", async () => {
    mockGet.mockResolvedValueOnce({
      exists: true,
      id: "txn-1",
      data: () => ({
        status: "pending",
        initiatorEmail: "max.ross@gmail.com",
        targetEmail: "daphne.ross@gmail.com",
        currentAmount: 5,
        description: "Dishes",
        events: [{ actorEmail: "daphne.ross@gmail.com" }],
      }),
    });
    mockUpdate.mockResolvedValueOnce(undefined);

    const res = await agent
      .post("/api/transactions/txn-1/reject")
      .set("Authorization", AUTH)
      .send({ message: "No thanks" });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(mockUpdate).toHaveBeenCalledWith(
      expect.objectContaining({ status: "rejected" })
    );
    expect(createNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        recipientEmail: "daphne.ross@gmail.com",
        type: "rejected",
      })
    );
  });
});

describe("POST /api/transactions/:id/counter", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    resetChainableMock();
  });

  it("returns 400 when amount < 1", async () => {
    const res = await agent
      .post("/api/transactions/txn-1/counter")
      .set("Authorization", AUTH)
      .send({ amount: 0 });

    expect(res.status).toBe(400);
    expect(res.body.error).toMatch(/at least 1/i);
  });

  it("returns 400 when max counters reached", async () => {
    mockGet.mockResolvedValueOnce({
      exists: true,
      id: "txn-1",
      data: () => ({
        status: "pending",
        initiatorEmail: "max.ross@gmail.com",
        targetEmail: "daphne.ross@gmail.com",
        initiatorCounters: 2,
        events: [{ actorEmail: "daphne.ross@gmail.com" }],
      }),
    });

    const res = await agent
      .post("/api/transactions/txn-1/counter")
      .set("Authorization", AUTH)
      .send({ amount: 3 });

    expect(res.status).toBe(400);
    expect(res.body.error).toMatch(/maximum/i);
  });

  it("counters successfully and creates notification", async () => {
    mockGet.mockResolvedValueOnce({
      exists: true,
      id: "txn-1",
      data: () => ({
        status: "pending",
        initiatorEmail: "max.ross@gmail.com",
        targetEmail: "daphne.ross@gmail.com",
        initiatorCounters: 1,
        description: "Dishes",
        events: [{ actorEmail: "daphne.ross@gmail.com" }],
      }),
    });
    mockUpdate.mockResolvedValueOnce(undefined);

    const res = await agent
      .post("/api/transactions/txn-1/counter")
      .set("Authorization", AUTH)
      .send({ amount: 7 });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(mockUpdate).toHaveBeenCalledWith(
      expect.objectContaining({
        currentAmount: 7,
        initiatorCounters: 2,
      })
    );
    expect(createNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        recipientEmail: "daphne.ross@gmail.com",
        type: "countered",
      })
    );
  });
});
