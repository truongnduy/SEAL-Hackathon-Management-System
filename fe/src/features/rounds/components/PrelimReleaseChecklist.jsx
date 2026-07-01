import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Alert, Button, Space, Table, Tag, Upload, message } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined, EyeOutlined } from '@ant-design/icons';
import { trackService } from '../../tracks/services/trackService';
import { mapTrackToFE } from '../../tracks/mappers/trackMapper';

const hasTrackProblem = (track) =>
  Boolean(track?.problem_statement_filename || track?.problem_statement_url);

const PrelimReleaseChecklist = ({ roundId, onReadyChange }) => {
  const [tracks, setTracks] = useState([]);
  const [loading, setLoading] = useState(false);
  const [uploadingTrackId, setUploadingTrackId] = useState(null);
  const [viewingTrackId, setViewingTrackId] = useState(null);

  const loadTracks = useCallback(async () => {
    if (!roundId) return;
    setLoading(true);
    try {
      const res = await trackService.listByRound(roundId);
      setTracks((Array.isArray(res) ? res : res?.items || []).map(mapTrackToFE));
    } catch (error) {
      message.error(error?.message || 'Không tải được danh sách bảng đấu');
      setTracks([]);
    } finally {
      setLoading(false);
    }
  }, [roundId]);

  useEffect(() => {
    loadTracks();
  }, [loadTracks]);

  const allReady = useMemo(
    () => tracks.length > 0 && tracks.every((t) => hasTrackProblem(t)),
    [tracks],
  );

  useEffect(() => {
    onReadyChange?.(allReady);
  }, [allReady, onReadyChange]);

  const handleViewPdf = async (trackId) => {
    setViewingTrackId(trackId);
    try {
      const blob = await trackService.getProblemStatement(trackId);
      const fileUrl = URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
      const opened = window.open(fileUrl, '_blank', 'noopener,noreferrer');
      if (!opened) {
        URL.revokeObjectURL(fileUrl);
        message.warning('Trình duyệt chặn popup — hãy cho phép để xem PDF.');
      }
    } catch {
      message.error('Không mở được file đề bài.');
    } finally {
      setViewingTrackId(null);
    }
  };

  const handleReplacePdf = async (trackId, file) => {
    if (!file) return Upload.LIST_IGNORE;
    setUploadingTrackId(trackId);
    try {
      await trackService.uploadProblemStatement(trackId, file);
      message.success('Đã cập nhật đề bài cho bảng đấu.');
      await loadTracks();
    } catch (error) {
      message.error(error?.message || 'Không upload được đề bài.');
    } finally {
      setUploadingTrackId(null);
    }
    return Upload.LIST_IGNORE;
  };

  const columns = [
    {
      title: 'Bảng đấu',
      dataIndex: 'name',
      key: 'name',
      render: (name) => <strong>{name}</strong>,
    },
    {
      title: 'Trạng thái đề',
      key: 'status',
      render: (_, record) =>
        hasTrackProblem(record) ? (
          <Tag color="success" icon={<CheckCircleOutlined />}>
            {record.problem_statement_filename || 'Đã có PDF'}
          </Tag>
        ) : (
          <Tag color="error" icon={<CloseCircleOutlined />}>
            Chưa upload
          </Tag>
        ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, record) => (
        <Space wrap>
          {hasTrackProblem(record) && (
            <Button
              size="small"
              icon={<EyeOutlined />}
              loading={viewingTrackId === record.id}
              onClick={() => handleViewPdf(record.id)}
            >
              Xem
            </Button>
          )}
          <Upload
            accept="application/pdf,.pdf"
            showUploadList={false}
            beforeUpload={(file) => handleReplacePdf(record.id, file)}
          >
            <Button size="small" loading={uploadingTrackId === record.id}>
              {hasTrackProblem(record) ? 'Đổi PDF' : 'Upload PDF'}
            </Button>
          </Upload>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Alert
        type="warning"
        showIcon
        style={{ marginBottom: 12 }}
        message="Nguyên tắc vàng"
        description={
          <span style={{ fontSize: 13 }}>
            Chỉ phát đề sau khi vòng đã <strong>kích hoạt</strong>. Sau khi bấm «Phát đề», thao tác{' '}
            <strong>one-way</strong> — không đổi file đề trên bảng đấu nữa. Mỗi đội chỉ nhận đề của bảng mình
            được phân.
          </span>
        }
      />
      <Table
        size="small"
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={tracks}
        pagination={false}
        locale={{ emptyText: 'Chưa có bảng đấu — tạo bảng và upload đề trước khi phát.' }}
      />
      {!loading && tracks.length > 0 && !allReady && (
        <Alert
          type="error"
          showIcon
          style={{ marginTop: 12 }}
          message="Chưa đủ đề bài"
          description="Mọi bảng đấu phải có PDF trước khi phát đề cho sinh viên."
        />
      )}
    </div>
  );
};

export default PrelimReleaseChecklist;
