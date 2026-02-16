import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth";
import NotificationBadge from "./NotificationBadge";

export default function Layout({ children }: { children: React.ReactNode }) {
  const { name, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <div className="min-h-screen">
      <nav className="bg-indigo-600 text-white shadow-md">
        <div className="max-w-3xl mx-auto px-4 py-3 flex items-center justify-between">
          <Link to="/" className="text-xl font-bold tracking-tight">
            Pardons
          </Link>
          <div className="flex items-center gap-4">
            <NotificationBadge />
            <span className="text-sm hidden sm:inline">{name}</span>
            <button
              onClick={handleLogout}
              className="text-sm bg-indigo-500 hover:bg-indigo-400 px-3 py-1 rounded"
            >
              Sign out
            </button>
          </div>
        </div>
      </nav>
      <main className="max-w-3xl mx-auto px-4 py-6">{children}</main>
    </div>
  );
}
