import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Divider, Space, Tag, Typography, message } from 'antd';
import { GithubOutlined } from '@ant-design/icons';
import { GoogleLogin } from '@react-oauth/google';
import { authService } from '../services/authService';
import { resolveOAuthError } from '../constants/oauthErrors';
import { GITHUB_OAUTH_MODE, startGithubOAuth } from '../utils/githubOAuth';

const { Text } = Typography;

const SocialLinkManager = () => {
  const [linkedState, setLinkedState] = useState({ GOOGLE: false, GITHUB: false });
  const [loadingProvider, setLoadingProvider] = useState(null);

  useEffect(() => {
    const fetchLinkedProviders = async () => {
      try {
        const res = await authService.getLinkedProviders();
        const list = Array.isArray(res) ? res : res?.data || [];
        setLinkedState({
          GOOGLE: list.includes('google'),
          GITHUB: list.includes('github'),
        });
      } catch (err) {
        console.error('Failed to load social linked providers:', err);
      }
    };
    fetchLinkedProviders();
  }, []);

  const updateState = (provider, linked) => {
    setLinkedState((prev) => ({ ...prev, [provider]: linked }));
  };

  const linkedSummary = useMemo(() => {
    const linkedProviders = Object.entries(linkedState)
      .filter(([, linked]) => linked)
      .map(([provider]) => provider);
    if (linkedProviders.length === 0) {
      return 'Chưa có tài khoản social nào được ghi nhận là đã liên kết.';
    }
    return `Đang ghi nhận liên kết: ${linkedProviders.join(', ')}.`;
  }, [linkedState]);

  const handleGoogleLink = async (idToken) => {
    if (!idToken) {
      message.error('Không nhận được idToken từ Google.');
      return;
    }
    setLoadingProvider('GOOGLE');
    try {
      const data = await authService.linkGoogle(idToken);
      updateState('GOOGLE', true);
      message.success(data?.message || 'Liên kết Google thành công.');
    } catch (error) {
      message.error(resolveOAuthError(error, 'Liên kết Google thất bại.').message);
    } finally {
      setLoadingProvider(null);
    }
  };

  const handleGithubLink = async () => {
    try {
      startGithubOAuth(GITHUB_OAUTH_MODE.LINK);
    } catch (error) {
      message.error(error.message || resolveOAuthError(error, 'Liên kết GitHub thất bại.').message);
    }
  };

  const handleUnlink = async (provider) => {
    setLoadingProvider(provider);
    try {
      const data =
        provider === 'GOOGLE'
          ? await authService.unlinkGoogle()
          : await authService.unlinkGithub();
      updateState(provider, false);
      message.success(data?.message || `Đã gỡ liên kết ${provider}.`);
    } catch (error) {
      message.error(resolveOAuthError(error, `Gỡ liên kết ${provider} thất bại.`).message);
    } finally {
      setLoadingProvider(null);
    }
  };

  return (
    <Space direction="vertical" size={12} style={{ width: '100%' }}>
      <Alert
        type="success"
        showIcon
        message="Liên kết tài khoản mạng xã hội"
        description="Trạng thái liên kết tài khoản được đồng bộ hóa trực tiếp từ máy chủ bảo mật."
      />

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Text strong>Google</Text>
        <Tag color={linkedState.GOOGLE ? 'green' : 'default'}>
          {linkedState.GOOGLE ? 'Đã liên kết' : 'Chưa liên kết'}
        </Tag>
      </div>
      {!linkedState.GOOGLE ? (
        <GoogleLogin
          onSuccess={(credentialResponse) => handleGoogleLink(credentialResponse?.credential)}
          onError={() => message.error('Không thể lấy token Google để liên kết.')}
          text="continue_with"
          shape="pill"
          width="100%"
        />
      ) : (
        <Button
          danger
          block
          loading={loadingProvider === 'GOOGLE'}
          onClick={() => handleUnlink('GOOGLE')}
        >
          Gỡ liên kết Google
        </Button>
      )}

      <Divider style={{ margin: '8px 0' }} />

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Text strong>GitHub</Text>
        <Tag color={linkedState.GITHUB ? 'green' : 'default'}>
          {linkedState.GITHUB ? 'Đã liên kết' : 'Chưa liên kết'}
        </Tag>
      </div>
      {!linkedState.GITHUB ? (
        <Button
          icon={<GithubOutlined />}
          block
          onClick={handleGithubLink}
        >
          Liên kết GitHub
        </Button>
      ) : (
        <Button
          danger
          block
          loading={loadingProvider === 'GITHUB'}
          onClick={() => handleUnlink('GITHUB')}
        >
          Gỡ liên kết GitHub
        </Button>
      )}

      <Text type="secondary" style={{ fontSize: 12 }}>
        {linkedSummary}
      </Text>

    </Space>
  );
};

export default SocialLinkManager;

