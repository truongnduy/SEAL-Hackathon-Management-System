import { Alert, Card, Col, Progress, Row, Space, Tag, Typography, theme } from 'antd';
import { CheckCircleFilled, ClockCircleOutlined, IdcardOutlined, MailOutlined, UserOutlined } from '@ant-design/icons';

const { Text, Title } = Typography;

const StudentProfileCard = ({ user }) => {
  const { token } = theme.useToken();
  const hasStudentCard = Boolean(user.studentCardUrl || user.studentCardUploaded);
  const checks = [
    { label: 'Thông tin cá nhân', done: Boolean(user.fullName && user.email), icon: <UserOutlined /> },
    { label: 'Email đăng nhập', done: Boolean(user.email), icon: <MailOutlined /> },
    { label: 'Thẻ sinh viên', done: hasStudentCard, icon: <IdcardOutlined /> },
    { label: 'Coordinator phê duyệt', done: user.status === 'APPROVED', icon: <CheckCircleFilled /> },
  ];
  const completedCount = checks.filter((item) => item.done).length;
  const progress = Math.round((completedCount / checks.length) * 100);

  return (
    <Card
      style={{ height: '100%', borderRadius: 16, border: `1px solid ${token.colorBorderSecondary}` }}
      styles={{ body: { padding: 20 } }}
    >
      <Space direction="vertical" size={18} style={{ width: '100%' }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>Mức độ sẵn sàng</Title>
          <Text type="secondary">Hoàn tất hồ sơ trước khi tham gia đội thi.</Text>
        </div>

        <Progress percent={progress} strokeColor={{ '0%': '#1677ff', '100%': '#13c2c2' }} />

        <Row gutter={[10, 10]}>
          {checks.map((item) => (
            <Col xs={24} sm={12} key={item.label}>
              <div
                style={{
                  padding: 12,
                  minHeight: 70,
                  borderRadius: 12,
                  border: `1px solid ${token.colorBorderSecondary}`,
                  background: item.done ? token.colorSuccessBg : token.colorWarningBg,
                }}
              >
                <Space align="start">
                  <span style={{ color: item.done ? token.colorSuccess : token.colorWarning }}>{item.icon}</span>
                  <div>
                    <Text strong style={{ display: 'block' }}>{item.label}</Text>
                    <Tag color={item.done ? 'success' : 'warning'} style={{ marginTop: 6, borderRadius: 999 }}>
                      {item.done ? 'Đã hoàn tất' : 'Cần bổ sung'}
                    </Tag>
                  </div>
                </Space>
              </div>
            </Col>
          ))}
        </Row>

        {user.status !== 'APPROVED' && (
          <Alert
            showIcon
            type="warning"
            icon={<ClockCircleOutlined />}
            message="Tài khoản đang chờ Coordinator phê duyệt"
            description="Bạn có thể kiểm tra và bổ sung hồ sơ. Các thao tác cần quyền sinh viên đã duyệt sẽ được mở sau khi tài khoản hợp lệ."
            style={{ borderRadius: 12 }}
          />
        )}
      </Space>
    </Card>
  );
};

export default StudentProfileCard;
