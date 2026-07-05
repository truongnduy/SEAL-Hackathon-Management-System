// src/features/presentation/components/PresentationReadinessPanel.tsx
import React, { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Alert, Spin, Tag, Typography } from 'antd';
import { personBApi, RoundSubmissionItem } from '../../../api/personB.api';
import {
  countGradableSubmissions,
  getSubmissionStatusMeta,
} from '../utils/presentationSubmissionUtils';

const { Text } = Typography;

interface PresentationReadinessPanelProps {
  roundId: number | null;
  trackId?: number | null;
  trackName?: string;
  canReviewLate?: boolean;
}

const PresentationReadinessPanel: React.FC<PresentationReadinessPanelProps> = ({
  roundId,
  trackId,
  trackName,
  canReviewLate = false,
}) => {
  const { data: submissions = [], isLoading } = useQuery<RoundSubmissionItem[]>({
    queryKey: ['roundSubmissions', roundId],
    queryFn: () => personBApi.getRoundSubmissions(roundId!),
    enabled: roundId != null && canReviewLate,
  });

  const scopedSubmissions = useMemo(() => {
    if (!trackId) return submissions;
    return submissions.filter((s) => s.track_id === trackId);
  }, [submissions, trackId]);

  const gradableCount = countGradableSubmissions(scopedSubmissions);

  if (!canReviewLate || !roundId) return null;

  return (
    <div
      style={{
        background: 'white',
        border: '1px solid #E5E7EB',
        borderRadius: '12px',
        padding: '16px',
        marginBottom: '16px',
      }}
    >
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          gap: '12px',
          marginBottom: '12px',
        }}
      >
        <div>
          <Text strong style={{ fontSize: '14px', display: 'block' }}>
            Tình trạng bài nộp{trackName ? ` — ${trackName}` : ''}
          </Text>
          <Text type="secondary" style={{ fontSize: '12px' }}>
            Chỉ bài <Text strong>đúng hạn / nộp trễ hợp lệ</Text> mới vào hàng đợi khi xáo trộn.
          </Text>
        </div>
      </div>

      {isLoading ? (
        <div style={{ textAlign: 'center', padding: '16px 0' }}>
          <Spin size="small" />
        </div>
      ) : scopedSubmissions.length === 0 ? (
        <Alert
          type="info"
          showIcon
          message="Chưa có bài nộp nào trên track này"
          description="Đội chưa nộp sẽ không xuất hiện khi xáo trộn."
        />
      ) : (
        <>
          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: '12px' }}>
            <Tag color="green">Vào queue: {gradableCount}</Tag>
            <Tag color="default">Tổng bài: {scopedSubmissions.length}</Tag>
          </div>

          {gradableCount < scopedSubmissions.length && (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: '12px' }}
              message={`${scopedSubmissions.length - gradableCount} bài chưa đủ điều kiện xáo trộn`}
              description="Yêu cầu đội hoàn thành nộp bài trước khi xáo trộn."
            />
          )}

          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {scopedSubmissions.map((sub) => {
              const meta = getSubmissionStatusMeta(sub.status);
              return (
                <div
                  key={sub.id}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '10px',
                    padding: '10px 12px',
                    border: '1px solid #F3F4F6',
                    borderRadius: '8px',
                    background: meta.gradable ? '#F0FDF4' : '#FFFBEB',
                  }}
                >
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <Text strong style={{ fontSize: '13px', display: 'block' }}>
                      {sub.team_name}
                    </Text>
                    <Text type="secondary" style={{ fontSize: '11px' }}>
                      #{sub.id}
                      {sub.track_name ? ` · ${sub.track_name}` : ''}
                    </Text>
                  </div>
                  <Tag color={meta.color}>{meta.label}</Tag>
                  <Tag color={meta.gradable ? 'success' : 'default'}>
                    {meta.gradable ? '✓ Queue' : '✗ Queue'}
                  </Tag>
                </div>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
};

export default PresentationReadinessPanel;
