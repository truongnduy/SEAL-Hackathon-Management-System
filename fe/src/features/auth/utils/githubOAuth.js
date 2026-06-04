const MODE_STORAGE_KEY = 'githubOauthMode';
const RETURN_TO_STORAGE_KEY = 'githubOauthReturnTo';
const CALLBACK_PATH = '/auth/github/callback';

export const GITHUB_OAUTH_MODE = {
  LOGIN: 'login',
  LINK: 'link',
};

export const getGithubRedirectUri = () => `${window.location.origin}${CALLBACK_PATH}`;

const getGithubAuthorizeUrl = () => {
  const clientId = import.meta.env.VITE_GITHUB_CLIENT_ID?.trim();
  if (!clientId) {
    throw new Error('Thiếu VITE_GITHUB_CLIENT_ID trong FE env.');
  }

  const scope = (import.meta.env.VITE_GITHUB_SCOPE || 'user:email').trim();

  const params = new URLSearchParams({
    client_id: clientId,
    redirect_uri: getGithubRedirectUri(),
    scope,
  });

  return `https://github.com/login/oauth/authorize?${params.toString()}`;
};

export const startGithubOAuth = (mode = GITHUB_OAUTH_MODE.LOGIN) => {
  sessionStorage.setItem(MODE_STORAGE_KEY, mode);
  sessionStorage.setItem(RETURN_TO_STORAGE_KEY, window.location.pathname + window.location.search);
  window.location.href = getGithubAuthorizeUrl();
};

export const getGithubOauthMode = () => {
  return sessionStorage.getItem(MODE_STORAGE_KEY) || GITHUB_OAUTH_MODE.LOGIN;
};

export const consumeGithubOauthContext = () => {
  const mode = getGithubOauthMode();
  const returnTo = sessionStorage.getItem(RETURN_TO_STORAGE_KEY) || '/';
  sessionStorage.removeItem(MODE_STORAGE_KEY);
  sessionStorage.removeItem(RETURN_TO_STORAGE_KEY);
  return { mode, returnTo };
};

const GITHUB_CODE_USED_PREFIX = 'github_oauth_code_used:';

export const getGithubCodeStorageKey = (code) => `${GITHUB_CODE_USED_PREFIX}${code}`;

/** @returns {'none' | 'pending' | 'success' | 'password_required'} */
export const getGithubCodeExchangeState = (code) => {
  const value = sessionStorage.getItem(getGithubCodeStorageKey(code));
  if (value === 'pending' || value === 'success' || value === 'password_required') {
    return value;
  }
  return 'none';
};

export const markGithubCodeExchangePending = (code) => {
  sessionStorage.setItem(getGithubCodeStorageKey(code), 'pending');
};

export const markGithubCodeExchangeSuccess = (code) => {
  sessionStorage.setItem(getGithubCodeStorageKey(code), 'success');
};

export const markGithubCodeExchangePasswordRequired = (code) => {
  sessionStorage.setItem(getGithubCodeStorageKey(code), 'password_required');
};

export const clearGithubCodeExchange = (code) => {
  sessionStorage.removeItem(getGithubCodeStorageKey(code));
};

