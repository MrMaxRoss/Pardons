import { createContext, useContext } from "react";

export interface AuthState {
  token: string | null;
  email: string | null;
  name: string | null;
  photoUrl: string | null;
  login: (token: string, email: string, name: string, photoUrl?: string) => void;
  logout: () => void;
}

export const AuthContext = createContext<AuthState>({
  token: null,
  email: null,
  name: null,
  photoUrl: null,
  login: () => {},
  logout: () => {},
});

export function useAuth() {
  return useContext(AuthContext);
}
