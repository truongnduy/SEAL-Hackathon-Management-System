import axiosClient from '../../../shared/api/axiosClient';
import { ENDPOINTS } from '../../../shared/api/endpoints';

const buildOptionalPasswordPayload = (payload, existingAccountPassword) => {
  if (!existingAccountPassword) {
    return payload;
  }
  return {
    ...payload,
    existingAccountPassword,
  };
};

/** Dedupe parallel POSTs (e.g. React Strict Mode) — same key shares one promise. */
const inflightPostByKey = new Map();

const postOnce = (cacheKey, postFn) => {
  if (inflightPostByKey.has(cacheKey)) {
    return inflightPostByKey.get(cacheKey);
  }

  const promise = postFn().finally(() => {
    inflightPostByKey.delete(cacheKey);
  });

  inflightPostByKey.set(cacheKey, promise);
  return promise;
};

const buildGithubExchangeKey = (action, code, redirectUri, existingAccountPassword) =>
  JSON.stringify([action, code, redirectUri, existingAccountPassword ?? '']);

export const persistAuthTokens = (authData) => {
  const accessToken = authData?.accessToken;
  const refreshToken = authData?.refreshToken;

  if (accessToken) {
    localStorage.setItem('accessToken', accessToken);
  }

  if (refreshToken) {
    localStorage.setItem('refreshToken', refreshToken);
  }
};

export const authService = {
  login: async (email, password) => {
    return axiosClient.post(ENDPOINTS.AUTH.LOGIN, { email, password });
  },

  register: async (payload) => {
    return axiosClient.post(ENDPOINTS.AUTH.REGISTER, payload);
  },

  logout: async (refreshToken) => {
    return axiosClient.post(ENDPOINTS.AUTH.LOGOUT, { refreshToken });
  },

  loginWithGoogle: async (idToken, existingAccountPassword) => {
    const payload = buildOptionalPasswordPayload({ idToken }, existingAccountPassword);
    return axiosClient.post(ENDPOINTS.AUTH.OAUTH_GOOGLE_LOGIN, payload);
  },

  loginWithGithubCode: async (code, redirectUri, existingAccountPassword) => {
    const cacheKey = buildGithubExchangeKey('github-login', code, redirectUri, existingAccountPassword);
    return postOnce(cacheKey, () => {
      const payload = buildOptionalPasswordPayload({ code, redirectUri }, existingAccountPassword);
      return axiosClient.post(ENDPOINTS.AUTH.OAUTH_GITHUB_LOGIN_CODE, payload);
    });
  },

  linkGoogle: async (idToken) => {
    return axiosClient.post(ENDPOINTS.AUTH.OAUTH_GOOGLE_LINK, { idToken });
  },

  linkGithubCode: async (code, redirectUri) => {
    const cacheKey = buildGithubExchangeKey('github-link', code, redirectUri, '');
    return postOnce(cacheKey, () =>
      axiosClient.post(ENDPOINTS.AUTH.OAUTH_GITHUB_LINK_CODE, { code, redirectUri })
    );
  },

  unlinkGoogle: async () => {
    return axiosClient.post(ENDPOINTS.AUTH.OAUTH_GOOGLE_UNLINK);
  },

  unlinkGithub: async () => {
    return axiosClient.post(ENDPOINTS.AUTH.OAUTH_GITHUB_UNLINK);
  },

  changePassword: async (currentPassword, newPassword) => {
    return axiosClient.post(ENDPOINTS.AUTH.CHANGE_PASSWORD, { currentPassword, newPassword });
  },
};
