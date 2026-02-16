import { useNavigate } from "react-router-dom";
import { GoogleLogin, CredentialResponse } from "@react-oauth/google";
import { useAuth } from "../auth";
import api from "../api";

const DEV_AUTH = import.meta.env.VITE_DEV_AUTH === "true";

const DEV_USERS = [
  { email: "max.ross@gmail.com", name: "Max" },
  { email: "daphne.ross@gmail.com", name: "Daphne" },
  { email: "violet.ross@gmail.com", name: "Violet" },
];

function parseJwt(token: string) {
  const base64Url = token.split(".")[1];
  const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
  const jsonPayload = decodeURIComponent(
    atob(base64)
      .split("")
      .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
      .join("")
  );
  return JSON.parse(jsonPayload);
}

export default function LoginPage() {
  const { login, token } = useAuth();
  const navigate = useNavigate();

  if (token) {
    navigate("/", { replace: true });
    return null;
  }

  const handleDevLogin = async (user: { email: string; name: string }) => {
    const devToken = `dev:${user.email}`;
    login(devToken, user.email, user.name);

    try {
      await api.post("/users/me", {});
    } catch {
      // non-critical
    }

    navigate("/");
  };

  const handleSuccess = async (response: CredentialResponse) => {
    const credential = response.credential;
    if (!credential) return;

    const payload = parseJwt(credential);
    login(credential, payload.email, payload.name, payload.picture);

    try {
      await api.post("/users/me", { photoUrl: payload.picture });
    } catch {
      // non-critical
    }

    navigate("/");
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-indigo-500 to-purple-600">
      <div className="bg-white rounded-2xl shadow-xl p-8 w-full max-w-sm text-center">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Pardons</h1>
        <p className="text-gray-500 mb-8">Ross Family Pardon Tracker</p>

        {DEV_AUTH ? (
          <div className="space-y-3">
            <p className="text-sm text-gray-400 mb-4">Dev mode — sign in as:</p>
            {DEV_USERS.map((user) => (
              <button
                key={user.email}
                onClick={() => handleDevLogin(user)}
                className="w-full py-3 px-4 rounded-lg border border-gray-300 text-gray-700 font-medium hover:bg-indigo-50 hover:border-indigo-300 transition-colors"
              >
                {user.name}
              </button>
            ))}
          </div>
        ) : (
          <div className="flex justify-center">
            <GoogleLogin
              onSuccess={handleSuccess}
              onError={() => console.error("Login failed")}
              theme="outline"
              size="large"
              shape="pill"
            />
          </div>
        )}
      </div>
    </div>
  );
}
