import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import api from "../api";
import { useAuth } from "../auth";
import { Transaction } from "../types";
import NumberPicker from "../components/NumberPicker";

const actionLabels: Record<string, string> = {
  created: "Created",
  accepted: "Accepted",
  rejected: "Rejected",
  countered: "Counter-offered",
};

export default function TransactionPage() {
  const { id } = useParams<{ id: string }>();
  const { email } = useAuth();
  const navigate = useNavigate();
  const [tx, setTx] = useState<Transaction | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionLoading, setActionLoading] = useState(false);
  const [showCounter, setShowCounter] = useState(false);
  const [counterAmount, setCounterAmount] = useState(1);
  const [counterMessage, setCounterMessage] = useState("");

  const fetchTransaction = async () => {
    try {
      const res = await api.get(`/transactions/${id}`);
      setTx(res.data);
    } catch {
      setError("Transaction not found");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransaction();
  }, [id]);

  if (loading) {
    return <div className="text-center py-12 text-gray-400">Loading...</div>;
  }
  if (error || !tx) {
    return <div className="text-center py-12 text-red-500">{error}</div>;
  }

  const isInitiator = tx.initiatorEmail === email;
  const lastEvent = tx.events[tx.events.length - 1];
  const isMyTurn = lastEvent && lastEvent.actorEmail !== email;
  const canAct = tx.status === "pending" && isMyTurn;

  const myCounters = isInitiator ? tx.initiatorCounters : tx.targetCounters;
  const canCounter = canAct && myCounters < 2;

  const handleAccept = async () => {
    setActionLoading(true);
    try {
      await api.post(`/transactions/${id}/accept`);
      await fetchTransaction();
    } catch (err: any) {
      setError(err.response?.data?.error || "Failed");
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async () => {
    setActionLoading(true);
    try {
      await api.post(`/transactions/${id}/reject`);
      await fetchTransaction();
    } catch (err: any) {
      setError(err.response?.data?.error || "Failed");
    } finally {
      setActionLoading(false);
    }
  };

  const handleCounter = async () => {
    setActionLoading(true);
    try {
      await api.post(`/transactions/${id}/counter`, {
        amount: counterAmount,
        message: counterMessage || undefined,
      });
      setShowCounter(false);
      setCounterMessage("");
      await fetchTransaction();
    } catch (err: any) {
      setError(err.response?.data?.error || "Failed");
    } finally {
      setActionLoading(false);
    }
  };

  const otherName = isInitiator ? tx.targetName : tx.initiatorName;

  return (
    <div>
      <button
        onClick={() => navigate("/")}
        className="text-sm text-indigo-600 hover:underline mb-4 inline-block"
      >
        &larr; Back
      </button>

      <div className="bg-white rounded-lg shadow-sm border p-6 mb-6">
        <div className="flex items-start justify-between mb-4">
          <div>
            <span className="text-xs font-medium text-gray-500 uppercase">
              {tx.type}
            </span>
            <h1 className="text-xl font-bold text-gray-900 mt-1">
              {tx.description}
            </h1>
            <p className="text-sm text-gray-500 mt-1">
              {isInitiator ? "To" : "From"}: {otherName}
            </p>
          </div>
          <div className="text-right">
            <p className="text-3xl font-bold text-indigo-600">
              {tx.currentAmount}
            </p>
            <p className="text-xs text-gray-400">pardon(s)</p>
            <span
              className={`inline-block mt-2 px-2 py-0.5 rounded text-xs font-medium ${
                tx.status === "pending"
                  ? "bg-yellow-100 text-yellow-800"
                  : tx.status === "accepted"
                  ? "bg-green-100 text-green-800"
                  : "bg-red-100 text-red-800"
              }`}
            >
              {tx.status}
            </span>
          </div>
        </div>

        {/* Timeline */}
        <div className="border-t pt-4">
          <h2 className="text-sm font-semibold text-gray-700 mb-3">Timeline</h2>
          <div className="space-y-3">
            {tx.events.map((event, i) => (
              <div key={i} className="flex gap-3">
                <div className="w-2 h-2 rounded-full bg-indigo-400 mt-2 shrink-0" />
                <div>
                  <p className="text-sm text-gray-800">
                    <span className="font-medium">
                      {event.actorEmail === email ? "You" : otherName}
                    </span>{" "}
                    {actionLabels[event.action] || event.action}
                    {event.action === "countered" && ` with ${event.amount}`}
                  </p>
                  {event.message && (
                    <p className="text-xs text-gray-500 mt-0.5 italic">
                      "{event.message}"
                    </p>
                  )}
                  <p className="text-xs text-gray-400 mt-0.5">
                    {event.timestamp?._seconds
                      ? new Date(event.timestamp._seconds * 1000).toLocaleString()
                      : event.timestamp
                      ? new Date(event.timestamp).toLocaleString()
                      : ""}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Actions */}
      {canAct && (
        <div className="space-y-3">
          {!showCounter ? (
            <div className="flex gap-3">
              <button
                onClick={handleAccept}
                disabled={actionLoading}
                className="flex-1 bg-green-600 text-white py-3 rounded-lg font-medium hover:bg-green-500 transition-colors disabled:opacity-50"
              >
                Accept
              </button>
              <button
                onClick={handleReject}
                disabled={actionLoading}
                className="flex-1 bg-red-600 text-white py-3 rounded-lg font-medium hover:bg-red-500 transition-colors disabled:opacity-50"
              >
                Reject
              </button>
              {canCounter && (
                <button
                  onClick={() => {
                    setCounterAmount(tx.currentAmount);
                    setShowCounter(true);
                  }}
                  disabled={actionLoading}
                  className="flex-1 bg-indigo-600 text-white py-3 rounded-lg font-medium hover:bg-indigo-500 transition-colors disabled:opacity-50"
                >
                  Counter ({2 - myCounters} left)
                </button>
              )}
            </div>
          ) : (
            <div className="bg-white rounded-lg shadow-sm border p-4 space-y-4">
              <h3 className="font-medium text-gray-900">Counter-offer</h3>
              <div>
                <label className="block text-sm text-gray-600 mb-1">
                  Amount
                </label>
                <NumberPicker
                  value={counterAmount}
                  onChange={setCounterAmount}
                />
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1">
                  Message (optional)
                </label>
                <input
                  type="text"
                  value={counterMessage}
                  onChange={(e) => setCounterMessage(e.target.value)}
                  placeholder="Why this amount?"
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
              <div className="flex gap-3">
                <button
                  onClick={handleCounter}
                  disabled={actionLoading}
                  className="flex-1 bg-indigo-600 text-white py-2 rounded-lg font-medium hover:bg-indigo-500 disabled:opacity-50"
                >
                  Send Counter
                </button>
                <button
                  onClick={() => setShowCounter(false)}
                  className="flex-1 border border-gray-300 text-gray-700 py-2 rounded-lg font-medium hover:bg-gray-50"
                >
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {tx.status === "pending" && !isMyTurn && (
        <p className="text-center text-sm text-gray-400 mt-4">
          Waiting for {otherName} to respond...
        </p>
      )}
    </div>
  );
}
