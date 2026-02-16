import { useNavigate } from "react-router-dom";
import { GoogleLogin, CredentialResponse } from "@react-oauth/google";
import { useAuth } from "../auth";
import api from "../api";

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
        <div className="flex justify-center">
          <GoogleLogin
            onSuccess={handleSuccess}
            onError={() => console.error("Login failed")}
            theme="outline"
            size="large"
            shape="pill"
          />
        </div>
      </div>
    </div>
  );
}
