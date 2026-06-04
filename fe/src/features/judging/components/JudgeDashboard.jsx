import React, { useState, useEffect } from 'react';
import { Typography, Row, Col, Card, Statistic, Button, Space, Tag, List, Progress, Skeleton, message } from 'antd';
import { 
  CheckCircleOutlined, 
  ClockCircleOutlined, 
  ArrowRightOutlined, 
  FundProjectionScreenOutlined, 
  BarChartOutlined, 
  TrophyOutlined, 
  CalendarOutlined 
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { judgeService } from '../services/judgeService';

const { Title, Text, Paragraph } = Typography;

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.4 } }
};

const JudgeDashboard = ({ user }) => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState({ stats: {}, assignments: [], upcomingEvents: [] });

  // Gọi API giả lập (Mock Data) từ judgeService
  useEffect(() => {
    judgeService.getDashboardStats().then(res => {
      if (res) setData(res);
      setLoading(false);
    });
  }, []);

  // Hiển thị khung xương (Skeleton) trong lúc chờ 0.6s
  if (loading) {
    return <div style={{ padding: '40px' }}><Skeleton active paragraph={{ rows: 6 }} /></div>;
  }

  return (
    <motion.div initial="hidden" animate="visible" variants={{ visible: { transition: { staggerChildren: 0.1 } } }} style={{ maxWidth: 1400, margin: '0 auto', fontFamily: "'Inter', sans-serif" }}>
      
      {/* 1. HERO BANNER */}
      <motion.div variants={itemVariants}>
        <div style={{
          background: 'linear-gradient(135deg, #1d4ed8 0%, #3b82f6 50%, #0ea5e9 100%)',
          borderRadius: '24px', padding: '40px', color: '#ffffff', marginBottom: '32px',
          boxShadow: '0 10px 25px -5px rgba(59, 130, 246, 0.4)', position: 'relative', overflow: 'hidden'
        }}>
          <div style={{ position: 'relative', zIndex: 2 }}>
            <Space align="center" style={{ marginBottom: 12 }}>
              <Tag color="cyan" style={{ borderRadius: 6, display: 'flex', alignItems: 'center', gap: 4, padding: '4px 12px', border: 'none', background: 'rgba(255,255,255,0.2)', color: '#fff', fontSize: 13, fontWeight: 600 }}>
                <TrophyOutlined /> Cổng Giám Khảo SEAL
              </Tag>
            </Space>
            <Title level={1} style={{ margin: 0, color: '#ffffff', fontSize: '32px', fontWeight: 800 }}>
              Xin chào {user?.fullName ? `Giám khảo ${user.fullName}` : 'Giám khảo'}!
            </Title>
            <Paragraph style={{ fontSize: '16px', color: 'rgba(255, 255, 255, 0.9)', marginTop: 8, maxWidth: '650px', lineHeight: 1.6 }}>
              Cảm ơn bạn đã tham gia đánh giá. Sự công tâm của bạn là chìa khóa tìm ra những dự án xuất sắc nhất cho cuộc thi.
            </Paragraph>
          </div>
        </div>
      </motion.div>

      {/* 2. THẺ THỐNG KÊ */}
      <motion.div variants={itemVariants}>
        <Row gutter={[24, 24]} style={{ marginBottom: 32 }}>
          <Col xs={24} sm={8}>
            <Card style={{ borderRadius: 16 }}>
              <Statistic title={<span style={{ fontWeight: 600, color: '#64748b' }}>Đã Chấm</span>} value={data.stats.totalEvaluated} prefix={<CheckCircleOutlined style={{color: '#10b981'}}/>} />
            </Card>
          </Col>
          <Col xs={24} sm={8}>
            <Card style={{ borderRadius: 16 }}>
              <Statistic title={<span style={{ fontWeight: 600, color: '#64748b' }}>Chờ Chấm</span>} value={data.stats.pendingEvaluations} prefix={<ClockCircleOutlined style={{color: '#f59e0b'}}/>} />
            </Card>
          </Col>
          <Col xs={24} sm={8}>
            <Card style={{ borderRadius: 16 }}>
              <Statistic title={<span style={{ fontWeight: 600, color: '#64748b' }}>Độ Lệch Chuẩn (RBL)</span>} value={data.stats.calibrationScore} suffix="/100" prefix={<BarChartOutlined style={{color: '#3b82f6'}}/>} />
            </Card>
          </Col>
        </Row>
      </motion.div>

      {/* 3. BỐ CỤC CHIA 2 CỘT: NHIỆM VỤ & LỊCH TRÌNH */}
      <Row gutter={[24, 24]}>
        
        {/* Cột trái (16/24): Nhiệm vụ Phân công */}
        <Col xs={24} lg={16}>
          <motion.div variants={itemVariants}>
            <Card title={<strong style={{ fontSize: '18px' }}><FundProjectionScreenOutlined style={{ color: '#3b82f6', marginRight: 8 }}/>Nhiệm vụ Phân công</strong>} style={{ borderRadius: 16 }}>
              <List
                dataSource={data.assignments}
                renderItem={item => (
                  <List.Item
                    style={{ padding: 24, border: '1px solid #f0f0f0', borderRadius: 16, marginBottom: 16, background: '#fafafa' }}
                    actions={[
                      <Button 
                        type={item.status === 'ONGOING' ? 'primary' : 'default'} 
                        style={{ borderRadius: 8 }} 
                        onClick={() => {
                          if (item.status === 'ONGOING') {
                            navigate(`/judging/${item.id}/scoring`);
                          } else if (item.status === 'UPCOMING') {
                            message.info(`Nhiệm vụ này chưa bắt đầu. Vui lòng quay lại sau!`);
                          } else {
                            message.warning(`Nhiệm vụ này đã kết thúc!`);
                          }
                        }}
                      >
                        {item.status === 'ONGOING' ? 'Vào phòng chấm thi' : 'Xem chi tiết'} <ArrowRightOutlined />
                      </Button>
                    ]}
                  >
                    <List.Item.Meta
                      title={<Title level={4} style={{ margin: 0 }}>{item.roundName} - {item.trackName}</Title>}
                      description={<Text type="secondary">{item.hackathonName} | Vai trò: <Tag color="blue" style={{ marginLeft: 4 }}>{item.role}</Tag></Text>}
                    />
                    <div style={{ width: 250 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                        <Text style={{ fontSize: 12 }}>Tiến độ: {item.scoredTeams}/{item.totalTeams}</Text>
                        <Text strong style={{ color: '#1677ff' }}>{item.progress}%</Text>
                      </div>
                      <Progress percent={item.progress} showInfo={false} strokeColor="#1677ff" />
                    </div>
                  </List.Item>
                )}
              />
            </Card>
          </motion.div>
        </Col>

        {/* Cột phải (8/24): Lịch trình */}
        <Col xs={24} lg={8}>
          <motion.div variants={itemVariants}>
            <Card title={<strong style={{ fontSize: '18px' }}><CalendarOutlined style={{ color: '#f59e0b', marginRight: 8 }}/>Lịch trình sắp tới</strong>} style={{ borderRadius: 16 }}>
               {data.upcomingEvents.map(event => (
                  <div key={event.id} style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
                    <div style={{ width: 12, height: 12, borderRadius: '50%', background: event.type === 'CALIBRATION' ? '#f59e0b' : '#3b82f6', marginTop: 4 }} />
                    <div>
                      <Text style={{ display: 'block', fontSize: 12, color: '#64748b' }}>{event.time}</Text>
                      <Text strong>{event.title}</Text>
                    </div>
                  </div>
                ))}
            </Card>
          </motion.div>
        </Col>

      </Row>
    </motion.div>
  );
};

export default JudgeDashboard;