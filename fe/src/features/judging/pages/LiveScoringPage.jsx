import React from 'react';
import { Row, Col, Card, Typography, List, Tag, Slider, InputNumber, Button, Input, Divider, Space, Spin, Empty, Badge } from 'antd';
import { ArrowLeftOutlined, CheckCircleFilled, SaveOutlined } from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { useLiveScoring } from '../hooks/useLiveScoring';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;

const getTypeColor = (type) => {
  switch (type?.toLowerCase()) {
    case 'technical': return 'blue';
    case 'innovation': return 'orange';
    case 'general': return 'green';
    default: return 'default';
  }
};

const LiveScoringPage = () => {
  const navigate = useNavigate();
  const { assignmentId } = useParams();
  
  const {
    teams, criteria, selectedTeam, setSelectedTeam,
    isLoading, isSubmitting, currentScores, handleScoreChange,
    comment, setComment, calculateTotalScore, submitFinalScore
  } = useLiveScoring(assignmentId);

  if (isLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', background: '#f5f7fa' }}>
        <Spin size="large" tip="Đang thiết lập phòng chấm thi..." />
      </div>
    );
  }

  return (
    <div style={{ padding: '24px', background: '#f5f7fa', minHeight: '100vh' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: 24 }}>
        <Button icon={<ArrowLeftOutlined />} type="text" onClick={() => navigate(-1)} style={{ marginRight: 16, background: '#fff', borderRadius: 8, height: 40, width: 40 }} />
        <div>
          <Title level={3} style={{ margin: 0, fontWeight: 700 }}>Phòng Chấm Thi (Live Scoring)</Title>
          <Text type="secondary">Mã phân công: #{assignmentId}</Text>
        </div>
      </div>

      <Row gutter={24}>
        <Col xs={24} lg={7}>
          <Card 
            title={<span style={{ fontWeight: 600 }}>Danh sách Đội thi ({teams.length})</span>} 
            style={{ borderRadius: 16, height: 'calc(100vh - 120px)', display: 'flex', flexDirection: 'column' }} 
            styles={{ body: { padding: 0, overflowY: 'auto', flex: 1 } }}
          >
            <List
              dataSource={teams}
              renderItem={team => {
                const isSelected = selectedTeam?.id === team.id;
                const isScored = team.status === 'SCORED';
                return (
                  <div
                    onClick={() => setSelectedTeam(team)}
                    style={{
                      padding: '16px 24px', cursor: 'pointer',
                      borderBottom: '1px solid #f0f0f0',
                      background: isSelected ? '#e6f4ff' : '#fff',
                      borderLeft: isSelected ? '4px solid #1677ff' : '4px solid transparent',
                      transition: 'all 0.2s ease'
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <Text strong style={{ fontSize: 16, color: isSelected ? '#1677ff' : '#1e293b' }}>{team.name}</Text>
                      {isScored ? <CheckCircleFilled style={{ color: '#10b981', fontSize: 18 }} /> : <Badge status="processing" color="#f59e0b" />}
                    </div>
                    <Text type="secondary" style={{ fontSize: 13, display: 'block', marginTop: 4 }}>Leader: {team.leader}</Text>
                    {isScored && (
                      <div style={{ marginTop: 8 }}>
                        <Tag color="success" style={{ margin: 0, fontWeight: 600, border: 'none', background: '#d1fae5', color: '#047857' }}>
                          Điểm: {team.totalScore}
                        </Tag>
                      </div>
                    )}
                  </div>
                );
              }}
            />
          </Card>
        </Col>

        <Col xs={24} lg={17}>
          <AnimatePresence mode="wait">
            {selectedTeam ? (
              <motion.div 
                key={selectedTeam.id} 
                initial={{ opacity: 0, y: 20 }} 
                animate={{ opacity: 1, y: 0 }} 
                exit={{ opacity: 0, y: -20 }}
                transition={{ duration: 0.3 }}
              >
                <Card style={{ borderRadius: 16, minHeight: 'calc(100vh - 120px)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24, padding: '20px 24px', background: '#f8fafc', borderRadius: 12, border: '1px solid #e2e8f0' }}>
                    <div>
                      <Text type="secondary" style={{ textTransform: 'uppercase', fontSize: 12, fontWeight: 600, letterSpacing: 1 }}>Đang chấm điểm cho</Text>
                      <Title level={2} style={{ margin: 0, color: '#0f172a', fontWeight: 800 }}>{selectedTeam.name}</Title>
                    </div>
                    <div style={{ textAlign: 'right', background: '#fff', padding: '12px 24px', borderRadius: 12, border: '1px solid #e2e8f0', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.05)' }}>
                      <Text type="secondary" style={{ display: 'block', fontSize: 12, fontWeight: 600 }}>TỔNG ĐIỂM</Text>
                      <Title level={1} style={{ margin: 0, color: '#f43f5e', fontWeight: 800, lineHeight: 1 }}>{calculateTotalScore()}</Title>
                    </div>
                  </div>

                  {criteria.length === 0 && <Empty description="Chưa có tiêu chí chấm điểm." style={{ margin: '60px 0' }} />}

                  {criteria.map((c, index) => {
                    const currentVal = currentScores[c.id] || 0;
                    const componentScore = (currentVal * (c.weight || 0)).toFixed(2);
                    
                    // BẢN VÁ LỖI TẠI ĐÂY: Xử lý an toàn biến maxScore
                    const maxPoint = c.maxScore || c.max_score || 10;
                    
                    return (
                      <div key={c.id} style={{ marginBottom: 24, padding: '24px', background: '#fff', borderRadius: 16, border: '1px solid #e2e8f0', boxShadow: '0 1px 3px rgba(0,0,0,0.02)' }}>
                        <Row align="middle" justify="space-between" style={{ marginBottom: 16 }}>
                          <Col span={16}>
                            <Space align="center" size="middle">
                              <Text strong style={{ fontSize: 18, color: '#1e293b' }}>{index + 1}. {c.name}</Text>
                              <Tag color={getTypeColor(c.type)} style={{ borderRadius: 4, fontWeight: 600 }}>{c.type}</Tag>
                            </Space>
                            <Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 0, fontSize: 14, lineHeight: 1.6 }}>{c.description}</Paragraph>
                          </Col>
                          
                          <Col span={8} style={{ textAlign: 'right' }}>
                            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end' }}>
                              <Space>
                                <InputNumber
                                  min={0} max={maxPoint}
                                  value={currentVal}
                                  onChange={(val) => handleScoreChange(c.id, val)}
                                  style={{ width: 80, fontSize: 16, fontWeight: 600, borderRadius: 8 }}
                                  size="large"
                                />
                                <Text type="secondary" style={{ fontSize: 16, fontWeight: 500 }}>/ {maxPoint}</Text>
                              </Space>
                              <Text style={{ marginTop: 8, fontSize: 13, color: '#64748b' }}>
                                Trọng số: <strong>{(c.weight * 100).toFixed(0)}%</strong> (Quy đổi: <strong style={{ color: '#1677ff' }}>{componentScore} đ</strong>)
                              </Text>
                            </div>
                          </Col>
                        </Row>

                        <Slider
                          min={0} max={maxPoint}
                          value={currentVal}
                          onChange={(val) => handleScoreChange(c.id, val)}
                          marks={{ 0: '0', [maxPoint]: maxPoint.toString() }}                          
                          tooltip={{ formatter: val => `${val} Điểm` }}
                          trackStyle={{ backgroundColor: '#1677ff', height: 6 }}
                          railStyle={{ height: 6 }}
                          handleStyle={{ height: 18, width: 18, marginTop: -6 }}
                        />
                      </div>
                    );
                  })}

                  <Divider />
                  <Title level={5} style={{ color: '#334155' }}>Nhận xét chuyên môn (Feedback)</Title>
                  <TextArea
                    rows={4}
                    placeholder="Nhập nhận xét chi tiết..."
                    value={comment}
                    onChange={(e) => setComment(e.target.value)}
                    style={{ borderRadius: 12, padding: 16, fontSize: 15 }}
                  />

                  <div style={{ marginTop: 32, display: 'flex', justifyContent: 'flex-end' }}>
                    <Space size="middle">
                      <Button size="large" style={{ borderRadius: 8, fontWeight: 600 }}>Lưu Nháp</Button>
                      <Button 
                        type="primary" size="large" 
                        icon={<SaveOutlined />} 
                        loading={isSubmitting} 
                        onClick={submitFinalScore}
                        style={{ background: '#10b981', borderColor: '#10b981', width: 160, borderRadius: 8, fontWeight: 600, boxShadow: '0 4px 14px rgba(16, 185, 129, 0.3)' }}
                      >
                        Chốt Điểm
                      </Button>
                    </Space>
                  </div>
                </Card>
              </motion.div>
            ) : (
              <Card style={{ borderRadius: 16, height: 'calc(100vh - 120px)', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#fff' }}>
                <Empty description="Vui lòng chọn một đội từ danh sách bên trái để bắt đầu chấm thi." />
              </Card>
            )}
          </AnimatePresence>
        </Col>
      </Row>
    </div>
  );
};

export default LiveScoringPage;