import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import axios from 'axios';
import * as authApi from './authApi';
import { AuthContext, type AuthContextValue } from './authContext';
import { clearSession, getTokenExpiration, isTokenExpired, loadSession, saveSession, type Session } from './session';

// Largest delay setTimeout accepts (2^31 - 1 ms)
const MAX_TIMEOUT_MS = 2_147_483_647;
// The access token is renewed this long before it expires, so requests never go out with an expired one
const REFRESH_MARGIN_MS = 60_000;
// Wait before trying again when the backend could not be reached to renew the session
const REFRESH_RETRY_MS = 30_000;

// A session can still be used while its access token is valid, or while it can be renewed with its refresh token
function isUsable(session: Session): boolean {
  return !isTokenExpired(session.token) || (!!session.refreshToken && !isTokenExpired(session.refreshToken));
}

function initialSession(): Session | null {
  const stored = loadSession();
  return stored && isUsable(stored) ? stored : null;
}

/**
 * Keeps track of the logged-in user for the whole app. Wrap the app with it and read it with useAuth().
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(initialSession);
  const token = session?.token;
  const refreshToken = session?.refreshToken;

  const logout = useCallback(() => setSession(null), []);

  const updateSession = useCallback((next: Session) => setSession(next), []);

  const login = useCallback(async (username: string, password: string) => {
    setSession(await authApi.login(username, password));
  }, []);

  const register = useCallback(async (username: string, email: string, password: string) => {
    setSession(await authApi.register(username, email, password));
  }, []);

  const loginWithGoogle = useCallback(async (credential: string) => {
    setSession(await authApi.loginWithGoogle(credential));
  }, []);

  // Persist the session and send the token with every axios request to the backend
  useEffect(() => {
    if (session) {
      saveSession(session);
      axios.defaults.headers.common.Authorization = `Bearer ${session.token}`;
    } else {
      clearSession();
      delete axios.defaults.headers.common.Authorization;
    }
  }, [session]);

  // Check with the backend that the token is still accepted (e.g. the user may no longer exist). An expired access
  // token is renewed first by the effect below, which checks the user too
  useEffect(() => {
    if (!token || isTokenExpired(token)) return;
    let cancelled = false;
    authApi
      .fetchCurrentUser(token)
      .then((user) => {
        if (!cancelled) setSession((current) => (current ? { ...current, user } : current));
      })
      .catch((error: unknown) => {
        // Only drop the session when the backend rejects it, not when it is just unreachable
        if (!cancelled && authApi.isUnauthorized(error)) setSession(null);
      });
    return () => {
      cancelled = true;
    };
  }, [token]);

  // Renew the session shortly before the access token expires, so the user stays logged in while the refresh token
  // is valid. Without a refresh token (or once it is rejected) the user is logged out when the access token expires
  useEffect(() => {
    if (!token) return;
    const expiration = getTokenExpiration(token);
    if (expiration === null) return;
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout>;

    const renew = () => {
      if (!refreshToken || isTokenExpired(refreshToken)) {
        logout();
        return;
      }
      authApi
        .refreshSession(refreshToken)
        .then((next) => {
          if (!cancelled) setSession(next);
        })
        .catch((error: unknown) => {
          if (cancelled) return;
          if (authApi.isUnauthorized(error)) logout();
          // Backend unreachable: try again later
          else timer = setTimeout(renew, REFRESH_RETRY_MS);
        });
    };

    const schedule = () => {
      const due = refreshToken ? expiration - REFRESH_MARGIN_MS : expiration;
      const remaining = due - Date.now();
      if (remaining <= 0) {
        renew();
        return;
      }
      // setTimeout fires at once for delays above ~24.8 days, so long waits are split into chunks
      timer = setTimeout(schedule, Math.min(remaining, MAX_TIMEOUT_MS));
    };
    schedule();
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [token, refreshToken, logout]);

  const value = useMemo<AuthContextValue>(
    () => ({ user: session?.user ?? null, login, register, loginWithGoogle, logout, updateSession }),
    [session, login, register, loginWithGoogle, logout, updateSession]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
