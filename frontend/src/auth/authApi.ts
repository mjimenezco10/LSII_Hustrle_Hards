import axios, { AxiosError } from 'axios';
import { translate } from '../i18n/translate';
import type { Language } from '../settings/settings';
import { getEnv } from '../utils/Env';

export type User = {
  id: number;
  username: string;
  email: string;
  // false for accounts created with Google that have not set a password yet. Missing in sessions saved before
  // these fields existed: treated as true until GET /auth/me refreshes the user
  hasPassword?: boolean;
  // Whether the account can log in with Google
  googleLinked?: boolean;
};

export type AuthResponse = {
  // Short-lived access token, sent with every request
  token: string;
  // Long-lived token that renews the session through POST /auth/refresh. Missing in sessions saved before it
  // existed: those end when their access token expires
  refreshToken?: string;
  user: User;
};

export type ApiError = {
  message: string;
  // Form field name -> error message, as returned by the backend
  fieldErrors: Record<string, string>;
};

const authUrl = () => `${getEnv().API_BASE_URL}/auth`;

export async function register(username: string, email: string, password: string): Promise<AuthResponse> {
  const response = await axios.post<AuthResponse>(`${authUrl()}/register`, { username, email, password });
  return response.data;
}

export async function login(username: string, password: string): Promise<AuthResponse> {
  const response = await axios.post<AuthResponse>(`${authUrl()}/login`, { username, password });
  return response.data;
}

/**
 * Exchanges the ID token given by the "Sign in with Google" button for a ProTube session.
 */
export async function loginWithGoogle(credential: string): Promise<AuthResponse> {
  const response = await axios.post<AuthResponse>(`${authUrl()}/google`, { credential });
  return response.data;
}

/**
 * Renews the session before its access token expires: answers a new access token and a new refresh token.
 */
export async function refreshSession(refreshToken: string): Promise<AuthResponse> {
  const response = await axios.post<AuthResponse>(`${authUrl()}/refresh`, { refreshToken });
  return response.data;
}

/**
 * Client id of "Sign in with Google" configured in the backend (ENV_PROTUBE_GOOGLE_CLIENT_ID); empty if disabled.
 */
export async function fetchGoogleClientId(): Promise<string> {
  const response = await axios.get<{ clientId?: string }>(`${authUrl()}/google/config`);
  return response.data?.clientId ?? '';
}

export async function fetchCurrentUser(token: string): Promise<User> {
  const response = await axios.get<User>(`${authUrl()}/me`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.data;
}

export function isUnauthorized(error: unknown): boolean {
  return (error as AxiosError).response?.status === 401;
}

/**
 * Converts any error thrown by the calls above into a message (and field errors) ready to show in a form. The
 * backend already answers in the language of the Accept-Language header; `language` is only used for the messages
 * written here (no connection, no message in the answer).
 */
export function toApiError(error: unknown, language: Language = 'en'): ApiError {
  const response = (error as AxiosError<Partial<ApiError>>).response;
  if (!response) {
    return { message: translate(language, 'common.connectionError'), fieldErrors: {} };
  }
  return {
    message: response.data?.message ?? translate(language, 'common.unexpectedError'),
    fieldErrors: response.data?.fieldErrors ?? {},
  };
}
