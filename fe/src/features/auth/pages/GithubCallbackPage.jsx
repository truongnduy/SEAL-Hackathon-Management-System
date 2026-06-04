import { useEffect, useRef, useState } from 'react';
import { Alert, Button, Card, Form, Input, Spin, Typography, message } from 'antd';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authService, persistAuthTokens } from '../services/authService';
import { resolveOAuthError } from '../constants/oauthErrors';
import {
  clearGithubCodeExchange,
  consumeGithubOauthContext,
  getGithubCodeExchangeState,
  getGithubRedirectUri,
  GITHUB_OAUTH_MODE,
  markGithubCodeExchangePasswordRequired,
  markGithubCodeExchangePending,
  markGithubCodeExchangeSuccess,
} from '../utils/githubOAuth';
import { ROUTES } from '../../../shared/constants/routes';

const { Title, Text } = Typography;

const GithubCallbackPage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [showPasswordForm, setShowPasswordForm] = useState(false);
  const [passwordLoading, setPasswordLoading] = useState(false);
  const [form] = Form.useForm();

  const code = searchParams.get('code');
  const githubError = searchParams.get('error');
  const githubErrorDescription = searchParams.get('error_description');

  const successRef = useRef(false);
  const contextRef = useRef(null);
  const effectRunIdRef = useRef(0);

  const redirectUri = getGithubRedirectUri();

  const finishLoginSuccess = (data) => {
    if (successRef.current) {
      return;
    }
    successRef.current = true;
    if (code) {
      markGithubCodeExchangeSuccess(code);
    }
    persistAuthTokens(data);

    // Persist minimal userInfo for onboarding detection
    try {
      const userInfo = { email: data?.email, status: data?.status, role: data?.role, userId: data?.userId };
      localStorage.setItem('userInfo', JSON.stringify(userInfo));
    } catch { /* no-op */ }

    message.success('Đăng nhập GitHub thành công!');
    window.location.replace(ROUTES.DASHBOARD);
  };

  const finishLinkSuccess = (returnTo) => {
    if (successRef.current) {
      return;
    }
    successRef.current = true;
    if (code) {
      markGithubCodeExchangeSuccess(code);
    }
    try {
      const localState = JSON.parse(localStorage.getItem('oauthLinkedStatus') || '{}');
      localStorage.setItem(
        'oauthLinkedStatus',
        JSON.stringify({ ...localState, GITHUB: true })
      );
    } catch {
      // no-op
    }
    message.success('Liên kết GitHub thành công!');
    window.location.replace(returnTo || ROUTES.DASHBOARD);
  };

  useEffect(() => {
    const runId = ++effectRunIdRef.current;
    const isActive = () => effectRunIdRef.current === runId;

    if (githubError) {
      setErrorMessage(
        githubErrorDescription
          ? `GitHub OAuth lỗi: ${githubErrorDescription}`
          : `GitHub OAuth lỗi: ${githubError}`
      );
      setLoading(false);
      return;
    }

    if (!code) {
      setErrorMessage('Không nhận được code từ GitHub callback.');
      setLoading(false);
      return;
    }

    const exchangeState = getGithubCodeExchangeState(code);

    if (exchangeState === 'success') {
      const hasToken = Boolean(localStorage.getItem('accessToken'));
      if (hasToken) {
        navigate(ROUTES.DASHBOARD, { replace: true });
      } else {
        setErrorMessage('Phiên đăng nhập GitHub đã hoàn tất. Vui lòng đăng nhập lại.');
        setLoading(false);
      }
      return;
    }

    if (exchangeState === 'password_required') {
      setShowPasswordForm(true);
      setLoading(false);
      return;
    }

    if (exchangeState === 'none') {
      markGithubCodeExchangePending(code);
    }

    const context = contextRef.current ?? consumeGithubOauthContext();
    contextRef.current = context;

    (async () => {
      try {
        if (context.mode === GITHUB_OAUTH_MODE.LINK) {
          await authService.linkGithubCode(code, redirectUri);
          if (isActive() && !successRef.current) {
            finishLinkSuccess(context.returnTo);
          }
          return;
        }

        const data = await authService.loginWithGithubCode(code, redirectUri);
        if (isActive() && !successRef.current) {
          finishLoginSuccess(data);
        }
      } catch (error) {
        if (!isActive() || successRef.current) {
          return;
        }

        const resolved = resolveOAuthError(error, 'Xử lý callback GitHub thất bại.');
        if (resolved.requiresPassword && context.mode !== GITHUB_OAUTH_MODE.LINK) {
          markGithubCodeExchangePasswordRequired(code);
          setShowPasswordForm(true);
        } else {
          clearGithubCodeExchange(code);
          setErrorMessage(resolved.message);
        }
      } finally {
        if (isActive()) {
          setLoading(false);
        }
      }
    })();
  }, [code, githubError, githubErrorDescription, navigate, redirectUri]);

  const handleConfirmPassword = async () => {
    if (!code || successRef.current) {
      return;
    }

    try {
      const values = await form.validateFields();
      setPasswordLoading(true);
      markGithubCodeExchangePending(code);

      const data = await authService.loginWithGithubCode(
        code,
        redirectUri,
        values.existingAccountPassword
      );

      if (!successRef.current) {
        finishLoginSuccess(data);
      }
    } catch (error) {
      if (error?.errorFields || successRef.current) {
        return;
      }
      const resolved = resolveOAuthError(error, 'Không thể hoàn tất đăng nhập GitHub.');
      clearGithubCodeExchange(code);
      setErrorMessage(resolved.message);
      setShowPasswordForm(false);
      setLoading(false);
    } finally {
      setPasswordLoading(false);
    }
  };

  if (loading) {
    return (
      <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}>
        <Card style={{ width: 420, textAlign: 'center' }}>
          <Spin />
          <div style={{ marginTop: 12 }}>Đang xử lý callback GitHub...</div>
        </Card>
      </div>
    );
  }

  return (
    <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', padding: 16 }}>
      <Card style={{ width: 460 }}>
        <Title level={4}>GitHub Callback</Title>

        {showPasswordForm ? (
          <>
            <Alert
              type="warning"
              showIcon
              message="Cần xác nhận mật khẩu"
              description="Backend yêu cầu existingAccountPassword để auto-link. Vui lòng nhập mật khẩu tài khoản hiện tại."
              style={{ marginBottom: 16 }}
            />
            <Form form={form} layout="vertical">
              <Form.Item
                label="Mật khẩu hiện tại"
                name="existingAccountPassword"
                rules={[{ required: true, message: 'Vui lòng nhập mật khẩu hiện tại' }]}
              >
                <Input.Password placeholder="Nhập mật khẩu..." />
              </Form.Item>
              <Button type="primary" block onClick={handleConfirmPassword} loading={passwordLoading}>
                Xác nhận và đăng nhập
              </Button>
            </Form>
          </>
        ) : (
          <>
            <Alert
              type="error"
              showIcon
              message="Không thể hoàn tất GitHub OAuth"
              description={errorMessage || 'Đã có lỗi xảy ra.'}
            />
            <div style={{ marginTop: 12 }}>
              <Text type="secondary">Bạn có thể quay lại trang đăng nhập và thử lại.</Text>
            </div>
            <Button style={{ marginTop: 16 }} onClick={() => navigate(ROUTES.LOGIN, { replace: true })}>
              Về đăng nhập
            </Button>
          </>
        )}
      </Card>
    </div>
  );
};

export default GithubCallbackPage;
