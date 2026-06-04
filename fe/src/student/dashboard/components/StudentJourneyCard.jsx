import { Card, Steps, Typography, theme } from 'antd';

const { Text, Title } = Typography;

const StudentJourneyCard = ({ user }) => {
  const { token } = theme.useToken();
  const currentStep = user.status === 'APPROVED' ? 1 : 0;

  return (
    <Card
      style={{ height: '100%', borderRadius: 16, border: `1px solid ${token.colorBorderSecondary}` }}
      styles={{ body: { padding: 20 } }}
    >
      <Title level={4} style={{ margin: 0 }}>Hành trình tham gia</Title>
      <Text type="secondary">Các bước chính dành cho sinh viên trong hệ thống.</Text>
      <Steps
        direction="vertical"
        size="small"
        current={currentStep}
        style={{ marginTop: 22 }}
        items={[
          { title: 'Hoàn thiện hồ sơ', description: 'Xác thực thông tin và chờ phê duyệt.' },
          { title: 'Thành lập hoặc tham gia đội', description: 'Tạo đội hoặc phản hồi lời mời.' },
          { title: 'Theo dõi trạng thái đội', description: 'Chờ Coordinator duyệt đội thi.' },
          { title: 'Sẵn sàng khai mạc', description: 'Track sẽ được Coordinator phân sau.' },
        ]}
      />
    </Card>
  );
};

export default StudentJourneyCard;
