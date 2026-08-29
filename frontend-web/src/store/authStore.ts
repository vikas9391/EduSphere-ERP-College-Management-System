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

/**
 * Some existing tenant data stores the student role as "students" while the
 * frontend routing/navigation expects the canonical "STUDENT" role. Normalize
 * this at the auth-store boundary so every page, route guard and sidebar sees a
 * consistent role without having to duplicate plural-role checks everywhere.
 */
function normalizeRole(role: string | undefined): string {
  const normalized = (role ?? "").trim().toUpperCase();
  if (normalized === "STUDENTS") return "STUDENT";
  if (normalized === "TEACHERS") return "TEACHER";
  return normalized;
}

function normalizeUser(user: User | null): User | null {
  if (!user) return null;
  return { ...user, role: normalizeRole(user.role) };
}

const storedUser = localStorage.getItem("user");
const initialUser = storedUser ? normalizeUser(JSON.parse(storedUser) as User) : null;

// Upgrade already-saved sessions too, so users do not have to sign out and back in
// just to get the corrected student navigation.
if (initialUser && storedUser) {
  localStorage.setItem("user", JSON.stringify(initialUser));
}

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem("token"),
  refreshToken: localStorage.getItem("refreshToken"),
  user: initialUser,

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
    const normalizedUser = normalizeUser(user);

    if (normalizedUser) {
      localStorage.setItem("user", JSON.stringify(normalizedUser));
    } else {
      localStorage.removeItem("user");
    }

    set({ user: normalizedUser });
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
