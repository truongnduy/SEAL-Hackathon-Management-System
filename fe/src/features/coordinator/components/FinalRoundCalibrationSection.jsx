import { useEffect, useState } from 'react';
import { Alert, Button, Card, Form, Input, InputNumber, List, Select, Space, Spin, Tag, Typography } from 'antd';
import { calibrationService } from '../../judging/services/calibrationService';
import axiosClient from '../../../shared/api/axiosClient';
import toast from 'react-hot-toast';

const { Title, Text } = Typography;

const parseList = (res) => (Array.isArray(res) ? res : res?.items || res?.data || []);

const FinalRoundCalibrationSection = ({ hackathonId: hackathonIdProp }) => {
  const [finalRound, setFinalRound] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [sampleSubmissions, setSampleSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm();

  const hackathonId = hackathonIdProp || JSON.parse(localStorage.getItem('userInfo') || '{}')?.hackathonId;

  const loadData = async () => {
    if (!hackathonId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const roundsRes = await axiosClient.get(`/api/v1/hackathons/${hackathonId}/rounds`);
      const rounds = parseList(roundsRes);
      const finalRnd = rounds.find((r) => r.is_final || r.isFinal);
      setFinalRound(finalRnd || null);

      if (finalRnd?.id && (finalRnd.is_active || finalRnd.isActive)) {
        const [sessionData, submissionRes] = await Promise.all([
          calibrationService.listByRound(finalRnd.id),
          axiosClient
            .get('/api/v1/submissions', { params: { roundId: finalRnd.id } })
            .catch(() => []),
        ]);
        setSessions(parseList(sessionData));
        setSampleSubmissions(parseList(submissionRes));
      } else {
        setSessions([]);
        setSampleSubmissions([]);
      }
    } catch {
      setSessions([]);
      setSampleSubmissions([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [hackathonId]);

  const handleCreate = async (values) => {
    if (!finalRound?.id) return;
    setSubmitting(true);
    try {
      await calibrationService.create({
        roundId: finalRound.id,
        sampleSubmissionId: values.sampleSubmissionId,
        targetScore: values.targetScore,
        instructions: values.instructions,
      });
      toast.success('Đã tạo phiên calibration.');
      form.resetFields();
      await loadData();
    } catch (err) {
      toast.error(err?.message || 'Không thể tạo phiên calibration.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleClose = async (sessionId) => {
    try {
      await calibrationService.update(sessionId, { status: 'CLOSED' });
      toast.success('Đã đóng phiên calibration.');
      await loadData();
    } catch (err) {
      toast.error(err?.message || 'Không thể đóng phiên.');
    }
  };

  if (loading) {
    return (
      <Card style={{ marginTop: 16 }}>
        <Spin />
      </Card>
    );
  }

  if (!finalRound) {
    return (
      <Alert
        style={{ marginTop: 16 }}
        type="info"
        showIcon
        message="Chưa có vòng Chung kết"
        description="Tạo và kích hoạt vòng CK trước khi cấu hình calibration."
      />
    );
  }

  const isActive = Boolean(finalRound.is_active || finalRound.isActive);
  if (!isActive) {
    return (
      <Alert
        style={{ marginTop: 16 }}
        type="warning"
        showIcon
        message="Vòng Chung kết chưa active"
        description="Kích hoạt vòng CK để tạo phiên calibration."
      />
    );
  }

  return (
    <Card style={{ marginTop: 16 }} title="Calibration GĐ5 (API)">
      <Form form={form} layout="vertical" onFinish={handleCreate}>
        <Form.Item
          name="sampleSubmissionId"
          label="Bài nộp mẫu (từ vòng Chung kết)"
          rules={[{ required: true, message: 'Chọn bài nộp mẫu' }]}
        >
          <Select
            placeholder="Chọn submission đã nộp ở CK"
            showSearch
            optionFilterProp="label"
            notFoundContent={
              sampleSubmissions.length === 0
                ? 'Chưa có bài nộp CK — student cần nộp trước.'
                : undefined
            }
            options={sampleSubmissions.map((sub) => {
              const id = sub.id ?? sub.submissionId;
              const teamName = sub.teamName || sub.team_name || sub.displayCode || `Team #${sub.teamId}`;
              return {
                value: id,
                label: `#${id} — ${teamName}`,
              };
            })}
          />
        </Form.Item>
        <Form.Item name="targetScore" label="Target score" rules={[{ required: true }]}>
          <InputNumber style={{ width: '100%' }} min={0} max={10} step={0.1} />
        </Form.Item>
        <Form.Item name="instructions" label="Hướng dẫn">
          <Input.TextArea rows={3} placeholder="Chấm thử bài mẫu trước khi chấm chính thức" />
        </Form.Item>
        <Button
          type="primary"
          htmlType="submit"
          loading={submitting}
          disabled={sampleSubmissions.length === 0}
        >
          Tạo phiên calibration
        </Button>
      </Form>

      <Title level={5} style={{ marginTop: 24 }}>
        Phiên hiện có
      </Title>
      {sessions.length === 0 ? (
        <Text type="secondary">Chưa có phiên nào.</Text>
      ) : (
        <List
          dataSource={sessions}
          renderItem={(session) => {
            const status = String(session.status || 'OPEN').toUpperCase();
            const isOpen = status === 'OPEN' || status === 'ACTIVE';
            return (
              <List.Item
                actions={
                  isOpen
                    ? [
                        <Button key="close" size="small" onClick={() => handleClose(session.id)}>
                          Đóng phiên
                        </Button>,
                      ]
                    : []
                }
              >
                <Space>
                  <Text>Mẫu #{session.sampleSubmissionId || session.sample_submission_id}</Text>
                  <Tag color={isOpen ? 'success' : 'default'}>{status}</Tag>
                </Space>
              </List.Item>
            );
          }}
        />
      )}
    </Card>
  );
};

export default FinalRoundCalibrationSection;
