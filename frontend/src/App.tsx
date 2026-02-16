import { useState, useCallback, useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthContext } from "./auth";
import { setTokenGetter } from "./api";
import LoginPage from "./pages/LoginPage";
import HomePage from "./pages/HomePage";
import NewPardonPage from "./pages/NewPardonPage";
import TransactionPage from "./pages/TransactionPage";
import Layout from "./components/Layout";

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const token = localStorage.getItem("token");
  if (!token) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

export default function App() {
  const [token, setToken] = useState<string | null>(
    localStorage.getItem("token")
  );
  const [email, setEmail] = useState<string | null>(
    localStorage.getItem("email")
  );
  const [name, setName] = useState<string | null>(
    localStorage.getItem("name")
  );
  const [photoUrl, setPhotoUrl] = useState<string | null>(
    localStorage.getItem("photoUrl")
  );

  const login = useCallback(
    (token: string, email: string, name: string, photoUrl?: string) => {
      setToken(token);
      setEmail(email);
      setName(name);
      setPhotoUrl(photoUrl || null);
      localStorage.setItem("token", token);
      localStorage.setItem("email", email);
      localStorage.setItem("name", name);
      if (photoUrl) localStorage.setItem("photoUrl", photoUrl);
    },
    []
  );

  const logout = useCallback(() => {
    setToken(null);
    setEmail(null);
    setName(null);
    setPhotoUrl(null);
    localStorage.clear();
  }, []);

  useEffect(() => {
    setTokenGetter(() => token);
  }, [token]);

  return (
    <AuthContext.Provider value={{ token, email, name, photoUrl, login, logout }}>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Layout>
                  <HomePage />
                </Layout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/new"
            element={
              <ProtectedRoute>
                <Layout>
                  <NewPardonPage />
                </Layout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/transaction/:id"
            element={
              <ProtectedRoute>
                <Layout>
                  <TransactionPage />
                </Layout>
              </ProtectedRoute>
            }
          />
        </Routes>
      </BrowserRouter>
    </AuthContext.Provider>
  );
}
