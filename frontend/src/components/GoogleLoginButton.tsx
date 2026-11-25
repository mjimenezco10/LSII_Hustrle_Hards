import { useEffect, useRef, useState } from 'react';
import { getEnv } from '../utils/Env';
import { loadGoogleIdentity } from '../auth/googleIdentity';
import { fetchGoogleClientId } from '../auth/authApi';
import { useTranslation } from '../i18n/i18nContext';

type Props = {
  // Receives the Google ID token once the user picks an account
  onCredential: (credential: string) => void;
  onError: (message: string) => void;
};

/**
 * Client id of "Sign in with Google": the one baked into the build (VITE_GOOGLE_CLIENT_ID) if any, otherwise the one
 * the backend is configured with (ENV_PROTUBE_GOOGLE_CLIENT_ID), so setting it only in the backend is enough.
 */
function useGoogleClientId(): string {
  const buildClientId = getEnv().GOOGLE_CLIENT_ID;
  const [backendClientId, setBackendClientId] = useState('');

  useEffect(() => {
    if (buildClientId) return;
    let cancelled = false;
    fetchGoogleClientId()
      .then((clientId) => {
        if (!cancelled) setBackendClientId(clientId);
      })
      // Backend unreachable or too old: Google login just stays hidden
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [buildClientId]);

  return buildClientId || backendClientId;
}

/**
 * "Sign in with Google" button rendered by Google's own library. Shows nothing if no client id is configured.
 */
const GoogleLoginButton = ({ onCredential, onError }: Props) => {
  const { t, language } = useTranslation();
  const clientId = useGoogleClientId();
  const containerRef = useRef<HTMLDivElement>(null);
  // Google keeps the first callback it gets, so it reads the latest props through refs
  const onCredentialRef = useRef(onCredential);
  const onErrorRef = useRef(onError);
  const unavailableRef = useRef(t('auth.googleUnavailable'));

  useEffect(() => {
    onCredentialRef.current = onCredential;
    onErrorRef.current = onError;
    unavailableRef.current = t('auth.googleUnavailable');
  });

  useEffect(() => {
    if (!clientId) return;
    let cancelled = false;
    loadGoogleIdentity()
      .then((google) => {
        if (cancelled || !containerRef.current) return;
        google.accounts.id.initialize({
          client_id: clientId,
          callback: (response) => onCredentialRef.current(response.credential),
        });
        // Drawn again in the new language when it changes
        containerRef.current.replaceChildren();
        google.accounts.id.renderButton(containerRef.current, {
          theme: 'outline',
          size: 'large',
          shape: 'pill',
          text: 'continue_with',
          width: 340,
          locale: language,
        });
      })
      .catch(() => {
        if (!cancelled) onErrorRef.current(unavailableRef.current);
      });
    return () => {
      cancelled = true;
    };
  }, [clientId, language]);

  if (!clientId) return null;

  return (
    <>
      <div className="auth-divider">{t('auth.or')}</div>
      <div ref={containerRef} className="auth-google" />
    </>
  );
};

export default GoogleLoginButton;
