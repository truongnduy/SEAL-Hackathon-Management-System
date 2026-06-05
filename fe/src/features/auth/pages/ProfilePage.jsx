import { useState, useEffect } from 'react';
import { Form, Input, Select, Button, message, Card, Row, Col, Space, Typography, Tag, Spin } from 'antd';
import { 
  UserOutlined, MailOutlined, PhoneOutlined, IdcardOutlined, 
  BankOutlined, SaveOutlined, KeyOutlined, SafetyCertificateOutlined 
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { userService } from '../services/userService';
import { ROUTES } from '../../../shared/constants/routes';
import SocialLinkManager from '../components/SocialLinkManager';

const { Title, Text } = Typography;
const { Option } = Select;

const CHAPTERS = [
  { id: 1, name: 'FPT Hà Nội' },
  { id: 2, name: 'FPT Hồ Chí Minh' },
  { id: 3, name: 'FPT Đà Nẵng' },
  { id: 4, name: 'FPT Cần Thơ' },
];

const ProfilePage = () => {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [userProfile, setUserProfile] = useState(null);
  const [userType, setUserType] = useState('INTERNAL');

  // Load user data on mount
  useEffect(() => {
    let active = true;
    const fetchProfile = async () => {
      try {
        const response = await userService.getMe();
        if (!active) return;
        
        const profile = response?.data || response || {};
        setUserProfile(profile);
        setUserType(profile.userType || 'INTERNAL');
        
        // Sync local storage userInfo with fresh info
        const stored = JSON.parse(localStorage.getItem('userInfo') || '{}');
        const merged = { ...stored, ...profile };
        localStorage.setItem('userInfo', JSON.stringify(merged));
        window.dispatchEvent(new Event('userInfoUpdated'));

        form.setFieldsValue({
          fullName: profile.fullName || '',
          userType: profile.userType || 'INTERNAL',
          phone: profile.phone || '',
          chapterId: profile.chapterId || 1,
          studentCode: profile.studentCode || '',
          institution: profile.institution || '',
        });
      } catch (err) {
        console.error('Failed to fetch user profile:', err);
        message.error('Không thể tải thông tin cá nhân.');
      } finally {
        if (active) setLoading(false);
      }
    };

    fetchProfile();
    return () => { active = false; };
  }, [form]);

  const handleSaveProfile = async (values) => {
    setSaving(true);
    try {
      const isStudent = userProfile?.role === 'STUDENT';
      const payload = {
        fullName: values.fullName,
        userType: isStudent ? values.userType : (userProfile?.userType || 'INTERNAL'),
        phone: values.phone || undefined,
        ...(isStudent
          ? (values.userType === 'INTERNAL'
            ? { studentCode: values.studentCode, chapterId: values.chapterId }
            : { institution: values.institution })
          : { chapterId: values.chapterId || undefined, institution: values.institution || undefined }),
      };

      const res = await userService.patchMe(payload);
      const updatedProfile = res?.data || res || {};
      
      setUserProfile((prev) => ({ ...prev, ...updatedProfile }));
      
      // Update local storage
      const stored = JSON.parse(localStorage.getItem('userInfo') || '{}');
      const merged = { ...stored, ...updatedProfile };
      localStorage.setItem('userInfo', JSON.stringify(merged));
      window.dispatchEvent(new Event('userInfoUpdated'));

      message.success('Cập nhật thông tin cá nhân thành công!');
    } catch (err) {
      console.error('Failed to update profile:', err);
      message.error(err?.message || err?.data?.error?.message || 'Có lỗi xảy ra khi cập nhật.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'grid', placeItems: 'center', minHeight: '60vh' }}>
        <Spin size="large" tip="Đang tải thông tin cá nhân..." />
      </div>
    );
  }

  const roleColors = {
    COORDINATOR: 'gold',
    ADMIN: 'red',
    JUDGE: 'blue',
    TEMP_JUDGE: 'purple',
    STUDENT: 'green',
    MENTOR: 'cyan'
  };

  return (
    <div style={{ padding: '24px', maxWidth: 1000, margin: '0 auto', fontFamily: "'Inter', sans-serif" }}>
      <Row gutter={[24, 24]}>
        {/* Left column: Quick actions, Status, Change Password, Social Accounts */}
        <Col xs={24} md={8}>
          <Space direction="vertical" size={24} style={{ width: '100%' }}>
            {/* Account Card */}
            <Card
              bordered={false}
              style={{
                borderRadius: 20,
                boxShadow: '0 4px 20px rgba(0,0,0,0.05)',
                textAlign: 'center',
                background: 'linear-gradient(135deg, #ffffff 0%, #f9fafb 100%)',
                border: '1px solid #e5e7eb'
              }}
            >
              <div style={{ position: 'relative', display: 'inline-block', marginBottom: 16 }}>
                <div style={{
                  width: 80,
                  height: 80,
                  borderRadius: '50%',
                  background: 'linear-gradient(90deg, #0072ff, #00e5ff)',
                  color: '#ffffff',
                  fontSize: 32,
                  fontWeight: 'bold',
                  display: 'grid',
                  placeItems: 'center',
                  margin: '0 auto',
                  boxShadow: '0 4px 10px rgba(0,114,255,0.2)'
                }}>
                  {userProfile?.fullName ? userProfile.fullName.charAt(0).toUpperCase() : 'U'}
                </div>
              </div>

              <Title level={4} style={{ margin: '0 0 4px 0', fontWeight: 'bold' }}>
                {userProfile?.fullName || 'Người dùng'}
              </Title>
              <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
                {userProfile?.email}
              </Text>

              <Space size={8} wrap style={{ justifyContent: 'center', marginBottom: 8 }}>
                <Tag color={roleColors[userProfile?.role] || 'blue'} style={{ borderRadius: 8, padding: '2px 8px', fontWeight: 'bold' }}>
                  {userProfile?.role}
                </Tag>
                <Tag color={userProfile?.status === 'APPROVED' ? 'success' : 'warning'} style={{ borderRadius: 8, padding: '2px 8px' }}>
                  {userProfile?.status === 'APPROVED' ? 'Đã duyệt' : 'Chờ duyệt'}
                </Tag>
              </Space>
            </Card>

            {/* Quick Actions Card */}
            <Card
              title={<span style={{ fontWeight: 'bold' }}>Tài khoản & Bảo mật</span>}
              bordered={false}
              style={{ borderRadius: 20, boxShadow: '0 4px 20px rgba(0,0,0,0.05)', border: '1px solid #e5e7eb' }}
            >
              <Button 
                block 
                type="dashed" 
                icon={<KeyOutlined />} 
                onClick={() => navigate(ROUTES.CHANGE_PASSWORD)}
                style={{ height: 40, borderRadius: 12 }}
              >
                Đổi mật khẩu tài khoản
              </Button>
            </Card>
          </Space>
        </Col>

        {/* Right column: Edit Profile Form & Linked Social Accounts */}
        <Col xs={24} md={16}>
          <Space direction="vertical" size={24} style={{ width: '100%' }}>
            {/* Personal Details Form */}
            <Card
              title={<span style={{ fontWeight: 'bold' }}>Thông tin cá nhân</span>}
              bordered={false}
              style={{ borderRadius: 20, boxShadow: '0 4px 20px rgba(0,0,0,0.05)', border: '1px solid #e5e7eb' }}
            >
              <Form
                form={form}
                layout="vertical"
                onFinish={handleSaveProfile}
                requiredMark={false}
              >
                <Row gutter={16}>
                  <Col xs={24} sm={12}>
                    <Form.Item label={<Text strong style={{ fontSize: 12 }}>EMAIL (CHỈ XEM)</Text>}>
                      <Input
                        value={userProfile?.email || ''}
                        readOnly
                        prefix={<MailOutlined style={{ color: '#0072ff' }} />}
                        style={{ height: 40, borderRadius: 12, backgroundColor: '#f3f4f6' }}
                      />
                    </Form.Item>
                  </Col>
                  
                  <Col xs={24} sm={12}>
                    <Form.Item
                      label={<Text strong style={{ fontSize: 12 }}>HỌ VÀ TÊN</Text>}
                      name="fullName"
                      rules={[{ required: true, message: 'Vui lòng nhập họ tên!' }]}
                    >
                      <Input
                        prefix={<UserOutlined style={{ color: '#0072ff' }} />}
                        placeholder="Nguyễn Văn A"
                        style={{ height: 40, borderRadius: 12 }}
                      />
                    </Form.Item>
                  </Col>
                </Row>

                {userProfile?.role === 'STUDENT' ? (
                  <>
                    <Row gutter={16}>
                      <Col xs={24} sm={12}>
                        <Form.Item label={<Text strong style={{ fontSize: 12 }}>ĐỐI TƯỢNG</Text>} name="userType">
                          <Select 
                            onChange={(val) => setUserType(val)}
                            style={{ height: 40 }}
                            dropdownStyle={{ borderRadius: 12 }}
                          >
                            <Option value="INTERNAL">Sinh viên FPT (Nội bộ)</Option>
                            <Option value="EXTERNAL">Sinh viên trường khác (Bên ngoài)</Option>
                          </Select>
                        </Form.Item>
                      </Col>

                      <Col xs={24} sm={12}>
                        <Form.Item label={<Text strong style={{ fontSize: 12 }}>SỐ ĐIỆN THOẠI</Text>} name="phone">
                          <Input
                            prefix={<PhoneOutlined style={{ color: '#0072ff' }} />}
                            placeholder="0912345678"
                            style={{ height: 40, borderRadius: 12 }}
                          />
                        </Form.Item>
                      </Col>
                    </Row>

                    {userType === 'INTERNAL' ? (
                      <Row gutter={16}>
                        <Col xs={24} sm={12}>
                          <Form.Item
                            label={<Text strong style={{ fontSize: 12 }}>CƠ SỞ (CHAPTER)</Text>}
                            name="chapterId"
                            rules={[{ required: true, message: 'Vui lòng chọn cơ sở!' }]}
                          >
                            <Select style={{ height: 40 }} dropdownStyle={{ borderRadius: 12 }}>
                              {CHAPTERS.map((c) => (
                                <Option key={c.id} value={c.id}>{c.name}</Option>
                              ))}
                            </Select>
                          </Form.Item>
                        </Col>

                        <Col xs={24} sm={12}>
                          <Form.Item
                            label={<Text strong style={{ fontSize: 12 }}>MÃ SINH VIÊN</Text>}
                            name="studentCode"
                            rules={[{ required: true, message: 'Vui lòng nhập mã sinh viên!' }]}
                          >
                            <Input
                              prefix={<IdcardOutlined style={{ color: '#0072ff' }} />}
                              placeholder="VD: SE123456"
                              style={{ height: 40, borderRadius: 12 }}
                            />
                          </Form.Item>
                        </Col>
                      </Row>
                    ) : (
                      <Row gutter={16}>
                        <Col xs={24}>
                          <Form.Item
                            label={<Text strong style={{ fontSize: 12 }}>TÊN TRƯỜNG / TỔ CHỨC</Text>}
                            name="institution"
                            rules={[{ required: true, message: 'Vui lòng nhập tên trường!' }]}
                          >
                            <Input
                              prefix={<BankOutlined style={{ color: '#0072ff' }} />}
                              placeholder="Đại học Bách Khoa..."
                              style={{ height: 40, borderRadius: 12 }}
                            />
                          </Form.Item>
                        </Col>
                      </Row>
                    )}
                  </>
                ) : (
                  <>
                    <Row gutter={16}>
                      <Col xs={24} sm={12}>
                        <Form.Item label={<Text strong style={{ fontSize: 12 }}>SỐ ĐIỆN THOẠI</Text>} name="phone">
                          <Input
                            prefix={<PhoneOutlined style={{ color: '#0072ff' }} />}
                            placeholder="0912345678"
                            style={{ height: 40, borderRadius: 12 }}
                          />
                        </Form.Item>
                      </Col>

                      <Col xs={24} sm={12}>
                        <Form.Item
                          label={<Text strong style={{ fontSize: 12 }}>CƠ SỞ / CHAPTER</Text>}
                          name="chapterId"
                        >
                          <Select style={{ height: 40 }} dropdownStyle={{ borderRadius: 12 }} placeholder="Chọn cơ sở trực thuộc (nếu có)" allowClear>
                            {CHAPTERS.map((c) => (
                              <Option key={c.id} value={c.id}>{c.name}</Option>
                            ))}
                          </Select>
                        </Form.Item>
                      </Col>
                    </Row>

                    <Row gutter={16}>
                      <Col xs={24}>
                        <Form.Item
                          label={<Text strong style={{ fontSize: 12 }}>ĐƠN VỊ CÔNG TÁC / TỔ CHỨC</Text>}
                          name="institution"
                        >
                          <Input
                            prefix={<BankOutlined style={{ color: '#0072ff' }} />}
                            placeholder="VD: FPT Software, Đại học FPT..."
                            style={{ height: 40, borderRadius: 12 }}
                          />
                        </Form.Item>
                      </Col>
                    </Row>
                  </>
                )}

                <div style={{ textAlign: 'right', marginTop: 12 }}>
                  <Button
                    type="primary"
                    htmlType="submit"
                    loading={saving}
                    icon={<SaveOutlined />}
                    style={{
                      height: 40,
                      borderRadius: 12,
                      padding: '0 24px',
                      fontWeight: 'bold',
                      background: 'linear-gradient(90deg, #0072ff, #00e5ff)',
                      border: 'none',
                      boxShadow: '0 4px 10px rgba(0,114,255,0.15)'
                    }}
                  >
                    Lưu thông tin
                  </Button>
                </div>
              </Form>
            </Card>

            {/* Social Accounts Card */}
            <Card
              title={<span style={{ fontWeight: 'bold' }}>Liên kết tài khoản mạng xã hội</span>}
              bordered={false}
              style={{ borderRadius: 20, boxShadow: '0 4px 20px rgba(0,0,0,0.05)', border: '1px solid #e5e7eb' }}
            >
              <SocialLinkManager />
            </Card>
          </Space>
        </Col>
      </Row>
    </div>
  );
};

export default ProfilePage;
