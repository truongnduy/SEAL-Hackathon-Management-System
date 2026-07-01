// src/features/judging/components/JudgeTimerAndInfo.jsx
import React, { useState } from 'react';
import { Card, Typography, Tag, Space, Button, Modal, Spin } from 'antd';
import { ClockCircleOutlined, GithubOutlined, FilePdfOutlined, TeamOutlined } from '@ant-design/icons';
import { judgeService } from '../services/judgeService';
import toast from 'react-hot-toast'; // <-- THÊM DÒNG NÀY ĐỂ HIỆN THÔNG BÁO

const { Title, Text } = Typography;

const JudgeTimerAndInfo = ({ logic, isFinal }) => {
  const { activeSlot, localTimerPhase, localRemainingSeconds } = logic;
  const [pdfModalOpen, setPdfModalOpen] = useState(false);
  const [pdfUrl, setPdfUrl] = useState(null);
  const [loadingPdf, setLoadingPdf] = useState(false);

  const formatTime = (secs) => {
    const m = Math.floor(Math.max(0, secs) / 60);
    const s = Math.max(0, secs) % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };

  const getTimerStyles = () => {
    switch(localTimerPhase) {
      case 'PRESENTING': return { bg: '#f0fdf4', border: '#bbf7d0', color: '#16a34a', text: 'THỜI GIAN THUYẾT TRÌNH' };
      case 'QA': return { bg: '#fffbeb', border: '#fde68a', color: '#d97706', text: 'THỜI GIAN HỎI ĐÁP' };
      case 'ENDED': return { bg: '#fef2f2', border: '#fecaca', color: '#dc2626', text: 'ĐÃ HẾT GIỜ' };
      default: return { bg: '#f8fafc', border: '#e2e8f0', color: '#94a3b8', text: 'ĐANG CHỜ' };
    }
  };

  const timerStyle = getTimerStyles();

  // XỬ LÝ MỞ PDF & BẮT LỖI TỪ BACKEND
  const handleViewPdf = async () => {
    if (!activeSlot?.submissionId) return;
    
    setPdfModalOpen(true);
    setLoadingPdf(true);
    
    try {
      const blob = await judgeService.getSubmissionSlide(activeSlot.submissionId);
      const url = URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
      setPdfUrl(url);
    } catch (error) {
      setPdfModalOpen(false); // Tắt modal loading đi
      
      // Bóc tách message lỗi từ API Backend (nếu có)
      const beMessage = error?.response?.data?.error?.message || error?.message;
      
      // Hiện thông báo lỗi rõ ràng cho Giám khảo
      toast.error(beMessage || "Không thể tải file PDF. Đội này có thể dùng Link thay vì nộp File!");
    } finally {
      setLoadingPdf(false);
    }
  };

  return (
    <>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 24, height: '100%' }}>
        
        {/* ĐỒNG HỒ ĐẾM NGƯỢC */}
        <Card style={{ borderRadius: 16, border: `2px solid ${timerStyle.border}`, background: timerStyle.bg, textAlign: 'center', boxShadow: '0 4px 12px rgba(0,0,0,0.02)' }} styles={{ body: { padding: '32px 24px' } }}>
          <Space align="center" style={{ marginBottom: 16 }}>
            <ClockCircleOutlined style={{ color: timerStyle.color, fontSize: 20 }} />
            <Text strong style={{ color: timerStyle.color, letterSpacing: 1 }}>{timerStyle.text}</Text>
          </Space>
          <div style={{ fontSize: 72, fontWeight: 900, fontFamily: 'monospace', color: timerStyle.color, lineHeight: 1 }}>
            {formatTime(localRemainingSeconds)}
          </div>
        </Card>

        {/* THÔNG TIN DỰ ÁN & TÀI LIỆU */}
        <Card title={<><TeamOutlined /> Thông tin Đội thi</>} style={{ borderRadius: 16, flex: 1 }} styles={{ header: { background: '#f8fafc' }}}>
          {activeSlot ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
              <div>
                <Text type="secondary" style={{ fontSize: 12, textTransform: 'uppercase', fontWeight: 600 }}>Mã Ẩn Danh</Text>
                <Title level={4} style={{ margin: 0, color: '#1e293b' }}>
                  {isFinal ? activeSlot.teamName : `TEAM-SBM#${activeSlot.submissionId}`}
                </Title>
              </div>

              {/* Nút Xem PDF */}
              <Button 
                type="primary" ghost block size="large" icon={<FilePdfOutlined />}
                onClick={handleViewPdf}
                style={{ height: 50, borderRadius: 10, fontWeight: 600, borderColor: '#ef4444', color: '#ef4444' }}
              >
                Xem Slide (PDF)
              </Button>

              {/* Nút Xem Github */}
              <Button 
                block size="large" icon={<GithubOutlined />}
                disabled={!activeSlot.repoUrl}
                onClick={() => window.open(activeSlot.repoUrl, '_blank')}
                style={{ height: 50, borderRadius: 10, fontWeight: 600, background: '#0f172a', color: '#fff', border: 'none' }}
              >
                Source Code (GitHub)
              </Button>
            </div>
          ) : (
            <Text type="secondary">Chưa có đội đang thi.</Text>
          )}
        </Card>
      </div>

      {/* MODAL HIỂN THỊ PDF */}
      <Modal
        title="Tài liệu thuyết trình (Slide PDF)"
        open={pdfModalOpen}
        onCancel={() => setPdfModalOpen(false)}
        footer={null} width={1000} style={{ top: 20 }}
      >
        <div style={{ height: '75vh', background: '#f0f2f5', borderRadius: 8, overflow: 'hidden', position: 'relative' }}>
          {loadingPdf ? (
            <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)' }}><Spin size="large" /></div>
          ) : (
             pdfUrl && <iframe src={`${pdfUrl}#toolbar=0`} width="100%" height="100%" style={{ border: 'none' }} />
          )}
        </div>
      </Modal>
    </>
  );
};

export default JudgeTimerAndInfo;