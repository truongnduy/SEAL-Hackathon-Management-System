export const OAUTH_ERROR_MESSAGES = {
  OAUTH_TOKEN_INVALID: 'Token OAuth không hợp lệ hoặc đã hết hạn.',
  OAUTH_ACCOUNT_NOT_LINKED: 'Tài khoản mạng xã hội chưa được liên kết.',
  OAUTH_ACCOUNT_ALREADY_LINKED: 'Tài khoản mạng xã hội này đã liên kết với người dùng khác.',
  OAUTH_EMAIL_MISMATCH: 'Email tài khoản mạng xã hội không trùng với email hiện tại.',
  OAUTH_EMAIL_NOT_VERIFIED: 'Email tài khoản mạng xã hội chưa được xác minh.',
  OAUTH_PASSWORD_CONFIRM_REQUIRED: 'Vui lòng nhập mật khẩu hiện tại để xác nhận liên kết tự động.',
  OAUTH_UNLINK_FORBIDDEN: 'Không thể gỡ liên kết social cuối cùng khi tài khoản chưa có mật khẩu.',
  UNAUTHORIZED: 'Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại.',
  FORBIDDEN: 'Bạn không có quyền thực hiện thao tác này.',
};

export const OAUTH_PASSWORD_REQUIRED_CODES = new Set([
  'OAUTH_PASSWORD_CONFIRM_REQUIRED',
]);

export const resolveOAuthError = (error, fallbackMessage) => {
  const code = error?.code || error?.data?.error?.code;
  const reason = error?.data?.error?.details?.reason;
  let message =
    (code && OAUTH_ERROR_MESSAGES[code]) ||
    error?.message ||
    fallbackMessage ||
    'Có lỗi xảy ra khi xử lý OAuth.';

  if (reason) {
    message = `${message} (${reason})`;
  }

  return {
    code,
    message,
    requiresPassword: OAUTH_PASSWORD_REQUIRED_CODES.has(code),
  };
};

