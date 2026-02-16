import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import api from "../api";
import { Transaction } from "../types";
import TransactionCard from "../components/TransactionCard";

type Tab = "pending" | "history";

export default function HomePage() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [tab, setTab] = useState<Tab>("pending");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    api
      .get("/transactions")
      .then((res) => setTransactions(res.data))
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const pending = transactions.filter((t) => t.status === "pending");
  const history = transactions.filter((t) => t.status !== "pending");
  const displayed = tab === "pending" ? pending : history;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div className="flex gap-1 bg-gray-100 rounded-lg p-1">
          <button
            onClick={() => setTab("pending")}
            className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
              tab === "pending"
                ? "bg-white shadow text-gray-900"
                : "text-gray-500 hover:text-gray-700"
            }`}
          >
            Pending ({pending.length})
          </button>
          <button
            onClick={() => setTab("history")}
            className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
              tab === "history"
                ? "bg-white shadow text-gray-900"
                : "text-gray-500 hover:text-gray-700"
            }`}
          >
            History ({history.length})
          </button>
        </div>
        <Link
          to="/new"
          className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-500 transition-colors"
        >
          + New Pardon
        </Link>
      </div>

      {loading ? (
        <div className="text-center py-12 text-gray-400">Loading...</div>
      ) : displayed.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-gray-400 mb-4">
            {tab === "pending"
              ? "No pending pardons"
              : "No history yet"}
          </p>
          {tab === "pending" && (
            <Link
              to="/new"
              className="text-indigo-600 font-medium hover:underline"
            >
              Create your first pardon
            </Link>
          )}
        </div>
      ) : (
        <div className="space-y-3">
          {displayed.map((tx) => (
            <TransactionCard key={tx.id} tx={tx} />
          ))}
        </div>
      )}
    </div>
  );
}
