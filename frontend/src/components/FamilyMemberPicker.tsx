import { useState, useEffect } from "react";
import api from "../api";
import { useAuth } from "../auth";
import { User } from "../types";

interface Props {
  value: string;
  onChange: (email: string, name: string) => void;
}

const FAMILY_MEMBERS: User[] = [
  { email: "max.ross@gmail.com", displayName: "Max" },
  { email: "daphne.ross@gmail.com", displayName: "Daphne" },
  { email: "violet.ross@gmail.com", displayName: "Violet" },
];

export default function FamilyMemberPicker({ value, onChange }: Props) {
  const { email: myEmail } = useAuth();
  const [members, setMembers] = useState<User[]>(FAMILY_MEMBERS);

  useEffect(() => {
    api
      .get("/users")
      .then((res) => {
        if (res.data.length > 0) setMembers(res.data);
      })
      .catch(() => {});
  }, []);

  const others = members.filter((m) => m.email !== myEmail);

  return (
    <div className="flex gap-2">
      {others.map((m) => (
        <button
          key={m.email}
          type="button"
          onClick={() => onChange(m.email, m.displayName)}
          className={`px-4 py-2 rounded-lg border text-sm font-medium transition-colors ${
            value === m.email
              ? "bg-indigo-600 text-white border-indigo-600"
              : "bg-white text-gray-700 border-gray-300 hover:border-indigo-300"
          }`}
        >
          {m.displayName}
        </button>
      ))}
    </div>
  );
}
