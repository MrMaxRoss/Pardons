import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { Response, NextFunction } from "express";

// Mock google-auth-library before importing auth middleware
vi.mock("google-auth-library", () => {
  const verifyIdToken = vi.fn();
  return {
    OAuth2Client: vi.fn().mockImplementation(() => ({
      verifyIdToken,
    })),
    __mockVerifyIdToken: verifyIdToken,
  };
});

import { authMiddleware, AuthRequest } from "../middleware/auth";
import { __mockVerifyIdToken } from "google-auth-library";

const mockVerify = __mockVerifyIdToken as ReturnType<typeof vi.fn>;

function createMockReqRes(authHeader?: string) {
  const req = {
    headers: { authorization: authHeader },
  } as unknown as AuthRequest;

  const res = {
    status: vi.fn().mockReturnThis(),
    json: vi.fn().mockReturnThis(),
  } as unknown as Response;

  const next = vi.fn() as NextFunction;

  return { req, res, next };
}

describe("authMiddleware", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("returns 401 when Authorization header is missing", async () => {
    const { req, res, next } = createMockReqRes(undefined);
    await authMiddleware(req, res, next);
    expect(res.status).toHaveBeenCalledWith(401);
    expect(next).not.toHaveBeenCalled();
  });

  it("returns 401 when Authorization header has no Bearer prefix", async () => {
    const { req, res, next } = createMockReqRes("Basic abc");
    await authMiddleware(req, res, next);
    expect(res.status).toHaveBeenCalledWith(401);
    expect(next).not.toHaveBeenCalled();
  });

  describe("dev mode", () => {
    it("sets email and name for valid dev token", async () => {
      const { req, res, next } = createMockReqRes("Bearer dev:max.ross@gmail.com");
      await authMiddleware(req, res, next);

      expect(next).toHaveBeenCalled();
      expect(req.userEmail).toBe("max.ross@gmail.com");
      expect(req.userName).toBe("Max");
    });

    it("returns 403 for unknown email in dev token", async () => {
      const { req, res, next } = createMockReqRes("Bearer dev:stranger@gmail.com");
      await authMiddleware(req, res, next);

      expect(res.status).toHaveBeenCalledWith(403);
      expect(next).not.toHaveBeenCalled();
    });
  });

  describe("production mode (Google token verification)", () => {
    const origDevAuth = process.env.DEV_AUTH;

    beforeEach(() => {
      process.env.DEV_AUTH = "false";
    });

    afterEach(() => {
      process.env.DEV_AUTH = origDevAuth;
    });

    it("passes through for a valid token with allowed email", async () => {
      mockVerify.mockResolvedValueOnce({
        getPayload: () => ({
          email: "max.ross@gmail.com",
          name: "Max Ross",
        }),
      });

      const { req, res, next } = createMockReqRes("Bearer valid-google-token");
      await authMiddleware(req, res, next);

      expect(next).toHaveBeenCalled();
      expect(req.userEmail).toBe("max.ross@gmail.com");
      expect(req.userName).toBe("Max Ross");
    });

    it("returns 403 for valid token with non-allowed email", async () => {
      mockVerify.mockResolvedValueOnce({
        getPayload: () => ({
          email: "stranger@gmail.com",
          name: "Stranger",
        }),
      });

      const { req, res, next } = createMockReqRes("Bearer valid-token");
      await authMiddleware(req, res, next);

      expect(res.status).toHaveBeenCalledWith(403);
      expect(next).not.toHaveBeenCalled();
    });

    it("returns 401 for invalid/expired token", async () => {
      mockVerify.mockRejectedValueOnce(new Error("Token expired"));

      const { req, res, next } = createMockReqRes("Bearer bad-token");
      await authMiddleware(req, res, next);

      expect(res.status).toHaveBeenCalledWith(401);
      expect(next).not.toHaveBeenCalled();
    });

    it("returns 401 when payload has no email", async () => {
      mockVerify.mockResolvedValueOnce({
        getPayload: () => ({ email: undefined }),
      });

      const { req, res, next } = createMockReqRes("Bearer no-email-token");
      await authMiddleware(req, res, next);

      expect(res.status).toHaveBeenCalledWith(401);
      expect(next).not.toHaveBeenCalled();
    });
  });
});
