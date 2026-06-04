import { Card, Col, Row, Space, Typography, theme } from 'antd';
import { ArrowRightOutlined, FileTextOutlined, MailOutlined, TeamOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';
import { ROUTES } from '../../../shared/constants/routes';

const { Text, Title } = Typography;

const ACTIONS = [
  {
    key: 'team',
    title: 'Đội thi của tôi',
    description: 'Tạo đội, mời thành viên và theo dõi trạng thái duyệt.',
    route: ROUTES.STUDENT_TEAM,
    icon: <TeamOutlined />,
    color: '#1677ff',
  },
  {
    key: 'invitations',
    title: 'Lời mời vào đội',
    description: 'Xem và phản hồi các lời mời đang chờ xử lý.',
    route: ROUTES.STUDENT_INVITATIONS,
    icon: <MailOutlined />,
    color: '#13c2c2',
  },
  {
    key: 'profile',
    title: 'Hồ sơ sinh viên',
    description: 'Kiểm tra thông tin cá nhân và trạng thái phê duyệt.',
    route: ROUTES.PROFILE,
    icon: <FileTextOutlined />,
    color: '#fa8c16',
  },
];

const StudentQuickActions = ({ onNavigate }) => {
  const { token } = theme.useToken();

  return (
    <section>
      <Title level={4} style={{ margin: '0 0 14px' }}>Đi nhanh đến công việc cần làm</Title>
      <Row gutter={[14, 14]}>
        {ACTIONS.map((action, index) => (
          <Col xs={24} md={8} key={action.key}>
            <motion.div
              initial={{ opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.08 * index, duration: 0.35 }}
              whileHover={{ y: -5 }}
              style={{ height: '100%' }}
            >
              <Card
                hoverable
                onClick={() => onNavigate(action.route)}
                style={{
                  height: '100%',
                  borderRadius: 16,
                  border: `1px solid ${token.colorBorderSecondary}`,
                  cursor: 'pointer',
                  boxShadow: '0 12px 28px rgba(15, 23, 42, 0.05)',
                }}
                styles={{ body: { padding: 20 } }}
              >
                <Space direction="vertical" size={14} style={{ width: '100%' }}>
                  <div
                    style={{
                      width: 44,
                      height: 44,
                      display: 'grid',
                      placeItems: 'center',
                      borderRadius: 12,
                      background: `${action.color}18`,
                      color: action.color,
                      fontSize: 21,
                    }}
                  >
                    {action.icon}
                  </div>
                  <div>
                    <Text strong style={{ display: 'block', fontSize: 16 }}>{action.title}</Text>
                    <Text type="secondary" style={{ display: 'block', marginTop: 6 }}>{action.description}</Text>
                  </div>
                  <Text strong style={{ color: action.color }}>
                    Mở trang <ArrowRightOutlined />
                  </Text>
                </Space>
              </Card>
            </motion.div>
          </Col>
        ))}
      </Row>
    </section>
  );
};

export default StudentQuickActions;
