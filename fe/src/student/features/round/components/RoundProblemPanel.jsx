import React, { useCallback, useEffect, useState } from 'react';
import { Alert, Button, Card, Spin, Typography, message } from 'antd';
import { FilePdfOutlined, DownloadOutlined } from '@ant-design/icons';
import { studentRoundService } from '../services/studentRound.service';

const { Title, Text } = Typography;

const openPdfBlob = (blob, filename) => {
  const fileUrl = URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
  const opened = window.open(fileUrl, '_blank', 'noopener,noreferrer');
  if (!opened) {
    URL.revokeObjectURL(fileUrl);
    message.warning('Trình duyệt chặn popup — hãy cho phép để xem PDF.');
    return;
  }
  setTimeout(() => URL.revokeObjectURL(fileUrl), 60_000);
};

const RoundProblemPanel = ({ team, hackathonId }) => {
  const [loading, setLoading] = useState(true);
  const [roundId, setRoundId] = useState(null);
  const [problem, setProblem] = useState(null);
  const [waitingRelease, setWaitingRelease] = useState(false);
  const [downloading, setDownloading] = useState(false);

  const loadProblem = useCallback(async () => {
    if (!team?.id || !team?.trackId || team?.status !== 'ACTIVE') {
      setLoading(false);
      setProblem(null);
      return;
    }

    setLoading(true);
    setWaitingRelease(false);
    try {
      const deadline = await studentRoundService.getCurrentDeadline(
        hackathonId || team?.hackathonId,
      );
      const activeRoundId = deadline?.roundId;
      setRoundId(activeRoundId);

      if (!activeRoundId) {
        setProblem(null);
        return;
      }

      if (!deadline?.problemReleased) {
        setWaitingRelease(true);
        setProblem(null);
        return;
      }

      const data = await studentRoundService.getProblem(activeRoundId);
      setProblem(data);
    } catch (error) {
      setProblem(null);
      if (error?.status === 404) {
        setWaitingRelease(true);
      }
    } finally {
      setLoading(false);
    }
  }, [team?.id, team?.trackId, team?.status, team?.hackathonId, hackathonId]);

  useEffect(() => {
    loadProblem();
  }, [loadProblem]);

  const handleDownload = async () => {
    if (!roundId) return;
    setDownloading(true);
    try {
      const blob = await studentRoundService.downloadProblemStatement(roundId);
      openPdfBlob(blob, problem?.problemFilename || 'de-bai.pdf');
    } catch (error) {
      message.error(error?.message || 'Không tải được đề bài.');
    } finally {
      setDownloading(false);
    }
  };

  if (!team?.trackId || team?.status !== 'ACTIVE') {
    return null;
  }

  if (loading) {
    return (
      <Card style={{ borderRadius: 16, textAlign: 'center', padding: '24px 0' }}>
        <Spin tip="Đang kiểm tra đề bài..." />
      </Card>
    );
  }

  if (waitingRelease) {
    return (
      <Card style={{ borderRadius: 16, border: '1px solid #ffe58f', background: '#fffbe6' }}>
        <Title level={5} style={{ marginTop: 0, color: '#ad6800' }}>
          Đề bài Sơ loại
        </Title>
        <Text style={{ color: '#ad6800' }}>
          Vòng thi đã kích hoạt. Coordinator sẽ phát đề cho bảng{' '}
          <strong>{team.trackName || 'của bạn'}</strong> — vui lòng chờ thông báo.
        </Text>
      </Card>
    );
  }

  if (!problem?.released) {
    return null;
  }

  return (
    <Card
      style={{ borderRadius: 16, border: '1px solid #bae0ff', background: '#f0f7ff' }}
      styles={{ body: { padding: 24 } }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16, flexWrap: 'wrap' }}>
        <div>
          <Title level={4} style={{ margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
            <FilePdfOutlined style={{ color: '#1677ff' }} />
            Đề bài Sơ loại
          </Title>
          <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
            Bảng đấu: <strong>{team.trackName || '—'}</strong>
          </Text>
          {problem.problemFilename && (
            <Text type="secondary" style={{ display: 'block', marginTop: 4 }}>
              File: {problem.problemFilename}
            </Text>
          )}
        </div>
        <Button
          type="primary"
          icon={<DownloadOutlined />}
          loading={downloading}
          onClick={handleDownload}
          style={{ borderRadius: 10, fontWeight: 600 }}
        >
          Xem / Tải đề
        </Button>
      </div>
      <Alert
        type="info"
        showIcon
        style={{ marginTop: 16, borderRadius: 8 }}
        message="Đề theo bảng đấu"
        description="Mỗi bảng có đề riêng — đội bạn chỉ thấy đề của bảng đã được phân qua lottery."
      />
    </Card>
  );
};

export default RoundProblemPanel;
