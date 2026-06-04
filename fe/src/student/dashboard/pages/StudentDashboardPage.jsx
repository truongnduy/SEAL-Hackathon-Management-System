import { Col, Row, Skeleton, Space } from 'antd';
import { useNavigate } from 'react-router-dom';
import StudentDashboardHero from '../components/StudentDashboardHero';
import StudentJourneyCard from '../components/StudentJourneyCard';
import StudentProfileCard from '../components/StudentProfileCard';
import StudentQuickActions from '../components/StudentQuickActions';
import { useStudentDashboard } from '../hooks/useStudentDashboard';

const StudentDashboardPage = () => {
  const navigate = useNavigate();
  const { user, isLoading, isRefreshing, refreshProfile } = useStudentDashboard();

  if (isLoading && !user.email) {
    return <Skeleton active paragraph={{ rows: 12 }} />;
  }

  return (
    <Space direction="vertical" size={24} style={{ width: '100%', maxWidth: 1280, margin: '0 auto' }}>
      <StudentDashboardHero user={user} isRefreshing={isRefreshing} onRefresh={refreshProfile} />
      <StudentQuickActions onNavigate={navigate} />
      <Row gutter={[16, 16]} align="stretch">
        <Col xs={24} xl={14}>
          <StudentProfileCard user={user} />
        </Col>
        <Col xs={24} xl={10}>
          <StudentJourneyCard user={user} />
        </Col>
      </Row>
    </Space>
  );
};

export default StudentDashboardPage;
