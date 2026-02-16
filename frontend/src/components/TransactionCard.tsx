import { Link } from "react-router-dom";
import { Transaction } from "../types";
import { useAuth } from "../auth";

const statusColors = {
  pending: "bg-yellow-100 text-yellow-800",
  accepted: "bg-green-100 text-green-800",
  rejected: "bg-red-100 text-red-800",
};

export default function TransactionCard({ tx }: { tx: Transaction }) {
  const { email } = useAuth();
  const isInitiator = tx.initiatorEmail === email;
  const otherName = isInitiator ? tx.targetName : tx.initiatorName;
  const lastEvent = tx.events[tx.events.length - 1];
  const isMyTurn = lastEvent && lastEvent.actorEmail !== email && tx.status === "pending";

  return (
    <Link
      to={`/transaction/${tx.id}`}
      className="block bg-white rounded-lg shadow-sm border p-4 hover:shadow-md transition-shadow"
    >
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2 mb-1">
            <span
              className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${statusColors[tx.status]}`}
            >
              {tx.status}
            </span>
            <span className="text-xs text-gray-500 capitalize">{tx.type}</span>
            {isMyTurn && (
              <span className="inline-block px-2 py-0.5 rounded text-xs font-medium bg-indigo-100 text-indigo-800">
                Your turn
              </span>
            )}
          </div>
          <p className="font-medium text-gray-900 truncate">{tx.description}</p>
          <p className="text-sm text-gray-500 mt-1">
            {isInitiator ? "To" : "From"}: {otherName}
          </p>
        </div>
        <div className="text-right shrink-0">
          <p className="text-2xl font-bold text-indigo-600">
            {tx.currentAmount}
          </p>
          <p className="text-xs text-gray-400">pardon(s)</p>
        </div>
      </div>
    </Link>
  );
}
