import React, { useState, useEffect } from 'react';
import { Table, Button, Space, Popconfirm, message, Card, Spin } from 'antd';
import { Plus, Edit, Trash2 } from 'lucide-react';
import TrackFormModal from '../components/TrackFormModal';
import StatusBadge from '../../../shared/components/ui/StatusBadge';
import { trackService } from '../services/trackService';
import { roundService } from '../../rounds/services/roundService';
import { mapRoundToFE } from '../../rounds/mappers/roundMapper';
import { mapTrackToFE, mapTrackToBE } from '../mappers/trackMapper';

const TrackManagementPage = ({ hackathonId }) => {
  const [tracks, setTracks] = useState([]);
  const [rounds, setRounds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [editingTrack, setEditingTrack] = useState(null);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [tracksRes, roundsRes] = await Promise.all([
        trackService.listByHackathon(hackathonId),
        roundService.listByHackathon(hackathonId)
      ]);
      
      const fullRounds = await Promise.all(
        (roundsRes || []).map(async (r) => {
          try {
            const detail = await roundService.getById(r.id);
            return mapRoundToFE(detail);
          } catch (e) {
            return mapRoundToFE(r);
          }
        })
      );

      setTracks((tracksRes || []).map(mapTrackToFE));
      setRounds(fullRounds);
    } catch (error) {
      message.error(error.message || 'Lỗi khi tải dữ liệu track');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [hackathonId]);

  const handleAdd = async () => {
    await fetchData();
    setEditingTrack(null);
    setIsModalVisible(true);
  };

  const handleEdit = async (trackSummary) => {
    try {
      setLoading(true);
      const trackDetail = await trackService.getById(trackSummary.id);
      setEditingTrack(mapTrackToFE(trackDetail));
      setIsModalVisible(true);
    } catch (error) {
      message.error(error.message || 'Lỗi khi tải chi tiết track');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    try {
      setLoading(true);
      await trackService.delete(id);
      message.success('Đã xóa track thành công');
      fetchData();
    } catch (error) {
      message.error(error.message || 'Lỗi khi xóa track');
      setLoading(false);
    }
  };

  const handleModalFinish = async (values) => {
    try {
      setLoading(true);
      const payload = mapTrackToBE(values);
      if (editingTrack) {
        await trackService.update(editingTrack.id, payload);
        message.success('Đã cập nhật track thành công');
      } else {
        await trackService.createByRound(values.round_id, payload);
        message.success('Đã tạo track mới thành công');
      }
      setIsModalVisible(false);
      await fetchData();
    } catch (error) {
      message.error(error.message || 'Lỗi khi lưu track');
      setLoading(false);
    }
  };

  const columns = [
    {
      title: 'Tên Track',
      dataIndex: 'name',
      key: 'name',
      render: (text) => <strong>{text}</strong>,
    },
    {
      title: 'Vòng sơ loại',
      dataIndex: 'round_id',
      key: 'round_id',
      render: (val) => rounds.find(r => r.id === val)?.name || '-',
    },
    {
      title: 'Số đội tối đa',
      key: 'teams',
      render: (_, record) => `${record.max_teams || 'Không giới hạn'}`,
    },
    {
      title: 'Sĩ số đội',
      key: 'team_size',
      render: (_, record) => `${record.min_team_size || '-'} - ${record.max_team_size || '-'} người`,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status) => <StatusBadge status={status} />,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, record) => (
        <Space size="middle">
          <Button 
            type="text" 
            icon={<Edit size={16} />} 
            onClick={() => handleEdit(record)}
          />
          <Popconfirm
            title="Xóa Track"
            description="Bạn có chắc chắn muốn xóa track này?"
            onConfirm={() => handleDelete(record.id)}
            okText="Xóa"
            cancelText="Hủy"
          >
            <Button type="text" danger icon={<Trash2 size={16} />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  if (loading && tracks.length === 0) {
    return <Card style={{ textAlign: 'center', padding: '40px 0' }}><Spin size="large" /></Card>;
  }

  const prelimRounds = rounds.filter(r => !r.is_final && r.round_type !== 'FINAL');

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'flex-end' }}>
        <Button 
          type="primary" 
          icon={<Plus size={16} />} 
          onClick={handleAdd}
          disabled={prelimRounds.length === 0}
        >
          Thêm Track
        </Button>
      </div>
      
      {prelimRounds.length === 0 && (
        <Card style={{ marginBottom: 16 }}>Vui lòng tạo ít nhất một Vòng sơ loại trước khi thêm Track.</Card>
      )}

      <Table scroll={{ x: 'max-content' }}
        columns={columns} 
        dataSource={tracks} 
        rowKey="id"
        pagination={false}
        loading={loading}
        locale={{ emptyText: 'No tracks found for this hackathon' }}
      />

      {isModalVisible && (
        <TrackFormModal
          visible={isModalVisible}
          title={editingTrack ? 'Edit Track' : 'Add Track'}
          initialValues={editingTrack}
          rounds={prelimRounds}
          isEditing={!!editingTrack}
          onCancel={() => setIsModalVisible(false)}
          onFinish={handleModalFinish}
        />
      )}
    </div>
  );
};

export default TrackManagementPage;
