import { Button } from 'antd';
import { GoogleLogin } from '@react-oauth/google';

const GOOGLE_LOGO_SRC = 'https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg';

const googleColorIcon = (
  <img src={GOOGLE_LOGO_SRC} alt="" width={18} height={18} style={{ display: 'block' }} />
);

/**
 * Nút Google cùng kiểu với GitHub; lớp GoogleLogin trong suốt xử lý OAuth (id token).
 */
export function GoogleSocialButton({ onSuccess, onError, buttonStyle, wrapperStyle, loading }) {
  return (
    <div
      style={{
        flex: '1 1 0',
        minWidth: 0,
        position: 'relative',
        minHeight: buttonStyle?.height ?? 40,
        ...wrapperStyle,
      }}
    >
      <Button
        block
        icon={googleColorIcon}
        style={{ ...buttonStyle, pointerEvents: 'none' }}
        loading={loading}
        aria-hidden
        tabIndex={-1}
      >
        Google
      </Button>
      <div
        style={{
          position: 'absolute',
          inset: 0,
          opacity: 0,
          overflow: 'hidden',
          cursor: 'pointer',
          zIndex: 1,
        }}
        aria-label="Đăng nhập bằng Google"
      >
        <GoogleLogin
          onSuccess={onSuccess}
          onError={onError}
          text="signin_with"
          shape="rectangular"
          size="large"
          width="400"
        />
      </div>
    </div>
  );
}
