import React, { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Card, Divider, List, Space, Spin, Tag, Typography, message } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import FinalRoundCalibrationSection from '../components/FinalRoundCalibrationSection';
import { hackathonService } from '../../hackathons/services/hackathonService';
import { roundService } from '../../rounds/services/roundService';
import { reviewService } from '../../review/services/reviewService';
import { ROUTES } from '../../../shared/constants/routes';

const { Title, Text } = Typography;

const FinalRoundConfigPage: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [hackathon, setHackathon] = useState<any>(null);
  const [rounds, setRounds] = useState<any[]>([]);
  const [readiness, setReadiness] = useState<any>(null);
  const navigate = useNavigate();

  const loadData = async () => {
    setLoading(true);
    try {
      const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
      let currentHackathon = null;
      if (userInfo?.hackathonId) {
        currentHackathon = await hackathonService.getById(userInfo.hackathonId);
      } else {
        const list = await hackathonService.search({ size: 20 });
        const items = Array.isArray(list) ? list : list?.items || list?.content || [];
        currentHackathon =
          items.find((item: any) => ['ONGOING', 'DRAFT'].includes(String(item.status).toUpperCase())) ||
          items[0] ||
          null;
      }
      if (!currentHackathon?.id) {
        setHackathon(null);
        setRounds([]);
        setReadiness(null);
        return;
      }

      const [roundList, readinessResult] = await Promise.all([
        roundService.listByHackathon(currentHackathon.id),
        reviewService.checkReadiness(currentHackathon.id, 'FINAL_ROUND'),
      ]);
      setHackathon(currentHackathon);
      setRounds(Array.isArray(roundList) ? roundList : roundList?.items || []);
      setReadiness(readinessResult?.data || readinessResult);
    } catch (error: any) {
      message.error(error?.message || 'Không tải được cấu hình Chung kết.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const finalRound = useMemo(
    () => rounds.find((round) => Boolean(round?.isFinal ?? round?.is_final)) || null,
    [rounds],
  );
  const blockers = readiness?.blockers || [];
  const warnings = readiness?.warnings || [];
  const isFinalReady = Boolean(readiness?.ready) && blockers.length === 0;
  const finalRoundActive = Boolean(finalRound?.isActive ?? finalRound?.is_active);

  const handleActivateFinal = async () => {
    if (!finalRound?.id) return;
    if (!isFinalReady) {
      return message.warning('Readiness FINAL_ROUND chưa đạt, vui lòng xử lý blocker trước.');
    }
    setSubmitting(true);
    try {
      await roundService.activate(finalRound.id, { note: 'Activate final round by coordinator' });
      message.success('Đã kích hoạt vòng Chung kết.');
      await loadData();
    } catch (error: any) {
      message.error(error?.message || 'Không thể kích hoạt vòng Chung kết.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <Card style={{ textAlign: 'center', padding: 32 }}>
        <Spin />
      </Card>
    );
  }

  if (!hackathon) {
    return <Alert showIcon type="warning" message="Chưa xác định được hackathon hiện tại." />;
  }

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Card>
        <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
          <div>
            <Title level={4} style={{ marginBottom: 4 }}>
              Cấu hình Chung kết (GĐ4 → GĐ5)
            </Title>
            <Text type="secondary">
              Màn này đã bỏ hoàn toàn mock/local-only, chỉ hiển thị dữ liệu thật từ BE.
            </Text>
          </div>
          <Button icon={<ReloadOutlined />} onClick={loadData}>
            Làm mới
          </Button>
        </Space>
      </Card>

      <Card title="Readiness FINAL_ROUND (Gate trước Activate CK)">
        <Space style={{ marginBottom: 12 }} wrap>
          <Tag color={isFinalReady ? 'success' : 'error'}>{isFinalReady ? 'READY' : 'NOT_READY'}</Tag>
          <Tag color="blue">Blockers: {blockers.length}</Tag>
          <Tag color="gold">Warnings: {warnings.length}</Tag>
          <Tag color={finalRoundActive ? 'green' : 'default'}>
            CK: {finalRoundActive ? 'ACTIVE' : 'INACTIVE'}
          </Tag>
        </Space>
        {blockers.length > 0 && (
          <Alert
            showIcon
            type="error"
            message="Blockers cần xử lý trước khi activate Chung kết"
            description={
              <List
                size="small"
                dataSource={blockers}
                renderItem={(item: any) => <List.Item>{item?.message || item?.code || 'Unknown blocker'}</List.Item>}
              />
            }
          />
        )}
        {warnings.length > 0 && (
          <Alert
            showIcon
            type="warning"
            style={{ marginTop: 12 }}
            message="Warnings (khuyến nghị xử lý)"
            description={
              <List
                size="small"
                dataSource={warnings}
                renderItem={(item: any) => <List.Item>{item?.message || item?.code || 'Unknown warning'}</List.Item>}
              />
            }
          />
        )}
        <Divider />
        <Space wrap>
          <Button type="primary" onClick={handleActivateFinal} loading={submitting} disabled={!finalRound || finalRoundActive || !isFinalReady}>
            Kích hoạt vòng Chung kết
          </Button>
          <Button onClick={() => navigate(ROUTES.HACKATHON_SETUP.replace(':hackathonId', String(hackathon.id)))}>
            Mở tab Nhân sự để gán giám khảo CK
          </Button>
        </Space>
      </Card>

      {finalRoundActive && (
        <Card title="Bước tiếp theo — GĐ5 Chung kết">
          <Alert
            showIcon
            type="success"
            message="Vòng Chung kết đã được kích hoạt"
            description="Hoàn thành các bước sau để kết thúc GĐ5 và chuyển sang GĐ6 (PENDING_CONFIRM)."
            style={{ marginBottom: 16 }}
          />
          <List
            size="small"
            dataSource={[
              'Phát đề Chung kết (tab Quản lý vòng thi → Phát đề)',
              'Tạo phiên calibration (tùy chọn — form bên dưới)',
              'Student các đội advanced nộp bài CK (multipart PDF, không trackId)',
              'Guest judge (FINAL_EXTERNAL) chấm điểm trên Judge Dashboard',
              'Khóa chấm CK → hackathon chuyển PENDING_CONFIRM',
            ]}
            renderItem={(item, index) => (
              <List.Item>
                <Text>
                  {index + 1}. {item}
                </Text>
              </List.Item>
            )}
          />
          <Divider />
          <Space wrap>
            <Button onClick={() => navigate(ROUTES.HACKATHON_SETUP.replace(':hackathonId', String(hackathon.id)))}>
              Quản lý vòng thi (phát đề / lock CK)
            </Button>
            <Button onClick={() => navigate(`/hackathons/${hackathon.id}/setup?tab=analytics`)}>
              RBL Dashboard (Analytics)
            </Button>
          </Space>
        </Card>
      )}

      <FinalRoundCalibrationSection hackathonId={hackathon.id} />
    </Space>
  );
};

export default FinalRoundConfigPage;
