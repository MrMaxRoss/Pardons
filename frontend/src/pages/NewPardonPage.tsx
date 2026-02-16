import { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api";
import FamilyMemberPicker from "../components/FamilyMemberPicker";
import NumberPicker from "../components/NumberPicker";

export default function NewPardonPage() {
  const navigate = useNavigate();
  const [type, setType] = useState<"offer" | "request">("offer");
  const [targetEmail, setTargetEmail] = useState("");
  const [targetName, setTargetName] = useState("");
  const [amount, setAmount] = useState(1);
  const [description, setDescription] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!targetEmail || !description.trim()) {
      setError("Please fill in all fields");
      return;
    }

    setSubmitting(true);
    setError("");
    try {
      const res = await api.post("/transactions", {
        type,
        targetEmail,
        targetName,
        amount,
        description: description.trim(),
      });
      navigate(`/transaction/${res.data.id}`);
    } catch (err: any) {
      setError(err.response?.data?.error || "Something went wrong");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">New Pardon</h1>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Type toggle */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            I want to...
          </label>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => setType("offer")}
              className={`flex-1 py-3 rounded-lg border text-sm font-medium transition-colors ${
                type === "offer"
                  ? "bg-indigo-600 text-white border-indigo-600"
                  : "bg-white text-gray-700 border-gray-300 hover:border-indigo-300"
              }`}
            >
              Offer a Pardon
            </button>
            <button
              type="button"
              onClick={() => setType("request")}
              className={`flex-1 py-3 rounded-lg border text-sm font-medium transition-colors ${
                type === "request"
                  ? "bg-indigo-600 text-white border-indigo-600"
                  : "bg-white text-gray-700 border-gray-300 hover:border-indigo-300"
              }`}
            >
              Request a Pardon
            </button>
          </div>
        </div>

        {/* Target */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            {type === "offer" ? "Offer to" : "Request from"}
          </label>
          <FamilyMemberPicker
            value={targetEmail}
            onChange={(email, name) => {
              setTargetEmail(email);
              setTargetName(name);
            }}
          />
        </div>

        {/* Amount */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            How many pardons?
          </label>
          <NumberPicker value={amount} onChange={setAmount} />
        </div>

        {/* Description */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            What's this for?
          </label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="e.g. For eating the last cookie"
            rows={3}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
          />
        </div>

        {error && (
          <p className="text-red-600 text-sm">{error}</p>
        )}

        <button
          type="submit"
          disabled={submitting}
          className="w-full bg-indigo-600 text-white py-3 rounded-lg font-medium hover:bg-indigo-500 transition-colors disabled:opacity-50"
        >
          {submitting
            ? "Sending..."
            : type === "offer"
            ? "Send Offer"
            : "Send Request"}
        </button>
      </form>
    </div>
  );
}
