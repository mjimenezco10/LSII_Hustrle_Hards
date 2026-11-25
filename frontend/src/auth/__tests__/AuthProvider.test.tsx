import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import axios from 'axios';
import { AuthProvider } from '../AuthProvider';
import { useAuth } from '../authContext';
import * as authApi from '../authApi';
import { loadSession, saveSession } from '../session';

jest.mock('../authApi', () => ({
  ...jest.requireActual('../authApi'),
  login: jest.fn(),
  register: jest.fn(),
  loginWithGoogle: jest.fn(),
  fetchCurrentUser: jest.fn(),
  refreshSession: jest.fn(),
}));
const mockedApi = authApi as jest.Mocked<typeof authApi>;

const user = { id: 1, username: 'alice', email: 'alice@mail.com' };
const tokenExpiringIn = (ms: number) =>
  `header.${btoa(JSON.stringify({ exp: Math.floor((Date.now() + ms) / 1000) }))}.signature`;

// Session returned by a change in the account settings
const renamedToken = tokenExpiringIn(60_000);
const renamedUser = { ...user, username: 'alicia' };

function Consumer() {
  const { user, login, register, loginWithGoogle, logout, updateSession } = useAuth();
  return (
    <>
      <span data-testid="user">{user?.username ?? 'anonymous'}</span>
      <button onClick={() => login('alice', 'password123')}>login</button>
      <button onClick={() => register('alice', 'alice@mail.com', 'password123')}>register</button>
      <button onClick={() => loginWithGoogle('google-id-token')}>google</button>
      <button onClick={logout}>logout</button>
      <button onClick={() => updateSession({ token: renamedToken, user: renamedUser })}>update</button>
    </>
  );
}

const renderWithProvider = () =>
  render(
    <AuthProvider>
      <Consumer />
    </AuthProvider>
  );

describe('AuthProvider', () => {
  beforeEach(() => {
    jest.resetAllMocks();
    jest.useRealTimers();
    localStorage.clear();
    delete axios.defaults.headers.common.Authorization;
    mockedApi.fetchCurrentUser.mockResolvedValue(user);
  });

  it('starts anonymous', () => {
    renderWithProvider();

    expect(screen.getByTestId('user')).toHaveTextContent('anonymous');
  });

  it('logs in, stores the session and sends the token with axios', async () => {
    const token = tokenExpiringIn(60_000);
    mockedApi.login.mockResolvedValueOnce({ token, user });
    renderWithProvider();

    fireEvent.click(screen.getByText('login'));

    await waitFor(() => expect(screen.getByTestId('user')).toHaveTextContent('alice'));
    expect(loadSession()?.token).toBe(token);
    expect(axios.defaults.headers.common.Authorization).toBe(`Bearer ${token}`);
  });

  it('registers and logs the new user in', async () => {
    mockedApi.register.mockResolvedValueOnce({ token: tokenExpiringIn(60_000), user });
    renderWithProvider();

    fireEvent.click(screen.getByText('register'));

    await waitFor(() => expect(screen.getByTestId('user')).toHaveTextContent('alice'));
  });

  it('replaces the session after a change in the account settings', async () => {
    mockedApi.login.mockResolvedValueOnce({ token: tokenExpiringIn(60_000), user });
    mockedApi.fetchCurrentUser.mockResolvedValue(renamedUser);
    renderWithProvider();
    fireEvent.click(screen.getByText('login'));
    await waitFor(() => expect(screen.getByTestId('user')).toHaveTextContent('alice'));

    fireEvent.click(screen.getByText('update'));

    await waitFor(() => expect(screen.getByTestId('user')).toHaveTextContent('alicia'));
    expect(loadSession()?.token).toBe(renamedToken);
    expect(axios.defaults.headers.common.Authorization).toBe(`Bearer ${renamedToken}`);
  });

  it('logs in with Google', async () => {
    mockedApi.loginWithGoogle.mockResolvedValueOnce({ token: tokenExpiringIn(60_000), user });
    renderWithProvider();

    fireEvent.click(screen.getByText('google'));

    await waitFor(() => expect(screen.getByTestId('user')).toHaveTextContent('alice'));
    expect(mockedApi.loginWithGoogle).toHaveBeenCalledWith('google-id-token');
  });

  it('logs out and forgets the session', async () => {
    saveSession({ token: tokenExpiringIn(60_000), user });
    renderWithProvider();

    fireEvent.click(screen.getByText('logout'));

    expect(screen.getByTestId('user')).toHaveTextContent('anonymous');
    expect(loadSession()).toBeNull();
    expect(axios.defaults.headers.common.Authorization).toBeUndefined();
  });

  it('restores a stored session on reload', async () => {
    saveSession({ token: tokenExpiringIn(60_000), user });
    renderWithProvider();

    expect(screen.getByTestId('user')).toHaveTextContent('alice');
    await waitFor(() => expect(mockedApi.fetchCurrentUser).toHaveBeenCalled());
  });

  it('ignores a stored session whose token already expired', () => {
    saveSession({ token: tokenExpiringIn(-60_000), user });
    renderWithProvider();

    expect(screen.getByTestId('user')).toHaveTextContent('anonymous');
  });

  it('drops the session when the backend rejects the token', async () => {
    mockedApi.fetchCurrentUser.mockRejectedValueOnce({ response: { status: 401 } });
    saveSession({ token: tokenExpiringIn(60_000), user });
    renderWithProvider();

    await waitFor(() => expect(screen.getByTestId('user')).toHaveTextContent('anonymous'));
  });

  it('keeps the session when the backend is just unreachable', async () => {
    mockedApi.fetchCurrentUser.mockRejectedValueOnce(new Error('Network Error'));
    saveSession({ token: tokenExpiringIn(60_000), user });
    renderWithProvider();

    await waitFor(() => expect(mockedApi.fetchCurrentUser).toHaveBeenCalled());
    expect(screen.getByTestId('user')).toHaveTextContent('alice');
  });

  it('logs out automatically when the token expires', async () => {
    jest.useFakeTimers();
    saveSession({ token: tokenExpiringIn(5_000), user });
    renderWithProvider();
    expect(screen.getByTestId('user')).toHaveTextContent('alice');

    await act(async () => {
      jest.advanceTimersByTime(6_000);
    });

    expect(screen.getByTestId('user')).toHaveTextContent('anonymous');
  });

  it('keeps the session for tokens that expire after the maximum setTimeout delay', async () => {
    jest.useFakeTimers();
    const day = 24 * 60 * 60 * 1000;
    saveSession({ token: tokenExpiringIn(30 * day), user });
    renderWithProvider();

    await act(async () => {
      jest.advanceTimersByTime(25 * day);
    });
    expect(screen.getByTestId('user')).toHaveTextContent('alice');

    await act(async () => {
      jest.advanceTimersByTime(6 * day);
    });
    expect(screen.getByTestId('user')).toHaveTextContent('anonymous');
  });

  it('renews the session before the access token expires', async () => {
    jest.useFakeTimers();
    const renewedToken = tokenExpiringIn(60 * 60_000);
    mockedApi.refreshSession.mockResolvedValueOnce({ token: renewedToken, refreshToken: 'refresh-2', user });
    saveSession({ token: tokenExpiringIn(90_000), refreshToken: tokenExpiringIn(60 * 60_000), user });
    renderWithProvider();

    await act(async () => {
      jest.advanceTimersByTime(31_000);
    });

    expect(mockedApi.refreshSession).toHaveBeenCalled();
    expect(screen.getByTestId('user')).toHaveTextContent('alice');
    expect(loadSession()?.token).toBe(renewedToken);
    expect(loadSession()?.refreshToken).toBe('refresh-2');
    expect(axios.defaults.headers.common.Authorization).toBe(`Bearer ${renewedToken}`);
  });

  it('restores and renews a stored session whose access token expired but whose refresh token is valid', async () => {
    const refreshToken = tokenExpiringIn(60 * 60_000);
    const renewedToken = tokenExpiringIn(60 * 60_000);
    mockedApi.refreshSession.mockResolvedValueOnce({ token: renewedToken, refreshToken, user });
    saveSession({ token: tokenExpiringIn(-60_000), refreshToken, user });
    renderWithProvider();

    expect(screen.getByTestId('user')).toHaveTextContent('alice');
    await waitFor(() => expect(loadSession()?.token).toBe(renewedToken));
    expect(mockedApi.refreshSession).toHaveBeenCalledWith(refreshToken);
  });

  it('ignores a stored session whose refresh token also expired', () => {
    saveSession({ token: tokenExpiringIn(-60_000), refreshToken: tokenExpiringIn(-1_000), user });
    renderWithProvider();

    expect(screen.getByTestId('user')).toHaveTextContent('anonymous');
  });

  it('logs out when the backend rejects the refresh token', async () => {
    mockedApi.refreshSession.mockRejectedValueOnce({ response: { status: 401 } });
    saveSession({ token: tokenExpiringIn(-60_000), refreshToken: tokenExpiringIn(60_000), user });
    renderWithProvider();

    await waitFor(() => expect(screen.getByTestId('user')).toHaveTextContent('anonymous'));
  });

  it('tries the renewal again when the backend is unreachable', async () => {
    jest.useFakeTimers();
    const renewedToken = tokenExpiringIn(60 * 60_000);
    mockedApi.refreshSession
      .mockRejectedValueOnce(new Error('Network Error'))
      .mockResolvedValueOnce({ token: renewedToken, refreshToken: 'refresh-2', user });
    saveSession({ token: tokenExpiringIn(-60_000), refreshToken: tokenExpiringIn(60 * 60_000), user });
    renderWithProvider();

    await act(async () => {
      await Promise.resolve();
    });
    expect(screen.getByTestId('user')).toHaveTextContent('alice');

    await act(async () => {
      jest.advanceTimersByTime(31_000);
    });

    expect(mockedApi.refreshSession).toHaveBeenCalledTimes(2);
    expect(loadSession()?.token).toBe(renewedToken);
  });

  it('fails when used outside the provider', () => {
    jest.spyOn(console, 'error').mockImplementation(() => {});

    expect(() => render(<Consumer />)).toThrow('useAuth must be used inside <AuthProvider>');
  });
});
