import { Button, Space, Tag, Typography, theme } from 'antd';
import { CheckCircleOutlined, ClockCircleOutlined, ReloadOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';

const { Paragraph, Text, Title } = Typography;

const StudentDashboardHero = ({ user, isRefreshing, onRefresh }) => {
  const { token } = theme.useToken();
  const isApproved = user.status === 'APPROVED';

  return (
    <motion.section
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.45, ease: 'easeOut' }}
      style={{
        position: 'relative',
        overflow: 'hidden',
        borderRadius: 24,
        padding: 'clamp(24px, 5vw, 42px)',
        color: '#fff',
        background: 'linear-gradient(135deg, #102a43 0%, #0f62fe 54%, #13c2c2 100%)',
        boxShadow: '0 24px 60px rgba(15, 98, 254, 0.22)',
      }}
    >
      <div
        style={{
          position: 'absolute',
          inset: 0,
          background: 'linear-gradient(115deg, rgba(255,255,255,0.16), transparent 40%)',
          pointerEvents: 'none',
        }}
      />
      <div style={{ position: 'relative', maxWidth: 760 }}>
        <Space size={10} wrap>
          <Tag
            icon={isApproved ? <CheckCircleOutlined /> : <ClockCircleOutlined />}
            color={isApproved ? 'success' : 'warning'}
            style={{ border: 0, borderRadius: 999, padding: '4px 10px', fontWeight: 700 }}
          >
            {isApproved ? 'Hồ sơ đã được phê duyệt' : 'Hồ sơ đang chờ phê duyệt'}
          </Tag>
          <Text style={{ color: 'rgba(255,255,255,0.78)', fontWeight: 700 }}>
            STUDENT WORKSPACE
          </Text>
        </Space>

        <Title level={1} style={{ color: '#fff', margin: '18px 0 8px', fontSize: 'clamp(28px, 5vw, 42px)' }}>
          Chào {user.fullName || 'bạn'}, sẵn sàng lập đội chưa?
        </Title>
        <Paragraph style={{ color: 'rgba(255,255,255,0.86)', margin: 0, maxWidth: 650, fontSize: 16 }}>
          Theo dõi hồ sơ, quản lý đội thi và xử lý lời mời trong một không gian gọn gàng dành riêng cho sinh viên.
        </Paragraph>

        <Button
          ghost
          icon={<ReloadOutlined spin={isRefreshing} />}
          loading={isRefreshing}
          onClick={() => onRefresh()}
          style={{
            marginTop: 24,
            borderRadius: token.borderRadiusLG,
            borderColor: 'rgba(255,255,255,0.62)',
            color: '#fff',
          }}
        >
          Đồng bộ hồ sơ
        </Button>
      </div>
    </motion.section>
  );
};

export default StudentDashboardHero;
