import { create } from "zustand";

/**
 * Fields come from com.collegeerp.Backend.auth.dto.LoginResponse (email, role,
 * tenantSchema) plus `id`, which LoginResponse does NOT include - it's only
 * present in the JWT's own claims, so LoginPage decodes the token once at login
 * time to pull it out. Everything else is trusted directly from the login
 * response body instead of being re-derived from the token, since that's the
 * backend's authoritative source for it.
 */
export interface User {
  id: number;
  email: string;
  role: string;
  tenantSchema: string;
}

interface AuthState {
  token: string | null;
  user: User | null;

  setToken: (token: string | null) => void;
  setUser: (user: User | null) => void;

  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem("token"),

  user: localStorage.getItem("user")
    ? JSON.parse(localStorage.getItem("user")!)
    : null,

  setToken: (token) => {
    if (token) {
      localStorage.setItem("token", token);
    } else {
      localStorage.removeItem("token");
    }

    set({ token });
  },

  setUser: (user) => {
    if (user) {
      localStorage.setItem("user", JSON.stringify(user));
    } else {
      localStorage.removeItem("user");
    }

    set({ user });
  },

  logout: () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");

    set({
      token: null,
      user: null,
    });
  },
}));
