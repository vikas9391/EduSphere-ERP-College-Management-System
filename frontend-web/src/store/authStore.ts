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
  /** From LoginResponse - only meaningful for staff/admin accounts. */
  mustChangePassword?: boolean;
  /** From the JWT's `permissions` claim - only present for staff/admin accounts
   *  (see com.collegeerp.Backend.common.Permission). Empty for teacher/student/
   *  super-admin, who don't go through the Role/Permission system. */
  permissions?: string[];
}

interface AuthState {
  token: string | null;
  refreshToken: string | null;
  user: User | null;

  setToken: (token: string | null) => void;
  setRefreshToken: (refreshToken: string | null) => void;
  setUser: (user: User | null) => void;

  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem("token"),
  refreshToken: localStorage.getItem("refreshToken"),

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

  setRefreshToken: (refreshToken) => {
    if (refreshToken) {
      localStorage.setItem("refreshToken", refreshToken);
    } else {
      localStorage.removeItem("refreshToken");
    }

    set({ refreshToken });
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
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("user");

    set({
      token: null,
      refreshToken: null,
      user: null,
    });
  },
}));

/** True if the signed-in user's role carries the given permission - always false for
 *  teacher/student/super-admin, whose `permissions` array is empty. Useful for hiding
 *  UI (e.g. "Add user") a signed-in staff member's role doesn't grant; the backend's
 *  own @PreAuthorize checks remain the real enforcement boundary either way. */
export function hasPermission(permission: string): boolean {
  return useAuthStore.getState().user?.permissions?.includes(permission) ?? false;
}

/** True if the signed-in user's role carries at least one of the given permissions -
 *  mirrors backend @PreAuthorize("hasAnyAuthority(...)") checks, e.g. RoleController's
 *  "any of CREATE_ROLE/EDIT_ROLE/DELETE_ROLE/ASSIGN_ROLE is enough to view roles". */
export function hasAnyPermission(permissions: string[]): boolean {
  const granted = useAuthStore.getState().user?.permissions ?? [];
  return permissions.some((p) => granted.includes(p));
}
