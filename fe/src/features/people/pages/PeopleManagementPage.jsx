// src/features/people/pages/PeopleManagementPage.jsx
import React, { useState } from 'react';
import { Card, Tabs, Button, Table, Form, Input, Modal, Select, Tag, Popconfirm, Alert, Typography, message } from 'antd';
import { UserPlus, Trash2 } from 'lucide-react';
import { usePeopleManagement } from '../hooks/usePeopleManagement';

const { Option } = Select;
const { Text } = Typography;

const PeopleManagementPage = ({ hackathonId }) => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [inviteForm] = Form.useForm();
  const [mentorForm] = Form.useForm();
  const [judgeForm] = Form.useForm();

  const { 
    mentors, judges, tempJudges, tracks, rounds, activeTeams, teamMentors, judgeAssignments, isLoading, 
    createTempJudge, assignMentor, removeMentor, assignJudge, removeJudge 
  } = usePeopleManagement(hackathonId);

  const checkIsFinalRound = (round) => {
    if (!round) return false;
    return !!round.is_final || round.isFinal || round.name.toLowerCase().includes('chung kết') || round.name.toLowerCase().includes('final');
  };

  const getRoundName = (id) => rounds.find(r => r.id === id)?.name || 'Không xác định';

  const renderJudgeRole = (role) => {
    switch (role) {
      case 'HEAD': return <Tag color="red">Trưởng ban</Tag>;
      case 'CALIBRATION': return <Tag color="gold">Chấm chéo</Tag>;
      case 'NORMAL': return <Tag color="blue">Bình thường</Tag>;
      default: return <Tag color="blue">Bình thường</Tag>;
    }
  };

  // Vòng Chung kết (FINAL) sẽ bị Backend chặn (trigger) nếu gán Mentor, nên ta lọc ngay trên Frontend
  const nonFinalRounds = rounds.filter(r => !checkIsFinalRound(r));
  
  // TỰ ĐỘNG LẤY ID CỦA VÒNG SƠ LOẠI LÀM MẶC ĐỊNH
  const prelimRoundId = nonFinalRounds.length > 0 ? nonFinalRounds[0].id : null;

  return (
    <div>
      <Card style={{ borderRadius: 12 }}>
        <Tabs defaultActiveKey="1" destroyInactiveTabPane>
          
          {/* TAB 1: KHÁCH MỜI */}
          <Tabs.TabPane tab="Giám khảo khách mời" key="1">
            <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'flex-end' }}>
              <Button type="primary" icon={<UserPlus size={16} />} onClick={() => setIsModalOpen(true)}>
                Mời Giám khảo
              </Button>
            </div>
            <Alert message="Danh sách Giám khảo được mời từ bên ngoài (Sẽ được gửi Email chứa Mật khẩu tạm 72h)." type="info" showIcon style={{ marginBottom: 16 }} />
            <Table dataSource={tempJudges} rowKey="id" pagination={false} loading={isLoading} columns={[
              { title: 'Tên', dataIndex: 'fullName', key: 'name', render: (t, r) => <strong>{t || r.name}</strong> },
              { title: 'Email', dataIndex: 'email', key: 'email' },
              { title: 'Tổ chức', dataIndex: 'institution', key: 'institution' },
              { title: 'Trạng thái', dataIndex: 'status', key: 'status', render: t => <Tag color={t === 'APPROVED' ? 'green' : 'orange'}>{t || 'PENDING'}</Tag> }
            ]} />
          </Tabs.TabPane>

          {/* TAB 2: MENTOR CHO ĐỘI */}
          <Tabs.TabPane tab="Phân công Mentor (Theo Đội thi)" key="2">
            <Card type="inner" style={{ marginBottom: 24, background: '#fafafa', borderRadius: 8 }}>
              <Form 
                layout="inline" 
                form={mentorForm}
                onFinish={(vals) => {
                  if (!prelimRoundId) {
                    return message.error('Hệ thống chưa có Vòng Sơ loại đang mở!');
                  }
                  // Tự động gắn cứng round_id là Vòng Sơ loại vào payload
                  const payload = { ...vals, round_id: prelimRoundId };
                  assignMentor(payload, () => mentorForm.resetFields());
                }} 
              >
                <Form.Item name="team_id" rules={[{ required: true, message: 'Chọn Đội thi' }]}>
                  <Select placeholder="-- Chọn Đội thi (Đã duyệt) --" style={{ width: 250 }} showSearch optionFilterProp="children">
                      {activeTeams.map(t => <Option key={t.id} value={t.id}>{t.teamName || t.team_name}</Option>)}
                  </Select>
                </Form.Item>

                {/* Dropdown: Chọn Mentor (Đã bọc Filter chống hiển thị Judge) */}
                <Form.Item name="mentor_id" rules={[{ required: true, message: 'Vui lòng chọn Mentor' }]}>
                  <Select placeholder="-- Chọn Người hướng dẫn --" style={{ width: 250 }} showSearch optionFilterProp="children">
                      {mentors
                        .filter(m => m.role === 'MENTOR') // Bức tường bảo vệ: Lọc bỏ ngay các role JUDGE, COORDINATOR...
                        .map(p => (
                          <Option key={p.id} value={p.id}>
                            {p.fullName || p.full_name || p.name}
                          </Option>
                        ))
                      }
                  </Select>
                </Form.Item>
                
                <Button type="primary" htmlType="submit" loading={isLoading}>Phân công</Button>
              </Form>
            </Card>
            <Table dataSource={teamMentors} rowKey="id" pagination={false} loading={isLoading} locale={{ emptyText: 'Chưa có Mentor nào được phân công' }} columns={[
              { title: 'Tên Đội thi', dataIndex: 'team_name', render: t => <strong>{t}</strong> },
              { title: 'Vòng thi', dataIndex: 'round_id', render: r => <Tag color="blue">{getRoundName(r)}</Tag> },
              { title: 'Mentor hỗ trợ', dataIndex: 'mentor_name', render: m => <Text strong style={{ color: '#1677ff' }}>{m}</Text> },
              { title: 'Thao tác', width: 100, align: 'right', render: (_, r) => (
                <Popconfirm title="Gỡ Mentor khỏi Đội thi?" onConfirm={() => removeMentor(r.team_id, r.round_id)}>
                  <Button type="text" danger icon={<Trash2 size={16}/>} />
                </Popconfirm>
              )}
            ]} />
          </Tabs.TabPane>

          {/* TAB 3: BAN GIÁM KHẢO */}
          <Tabs.TabPane tab="Phân công Ban Giám Khảo" key="3">
            <Card type="inner" style={{ marginBottom: 24, background: '#fafafa', borderRadius: 8 }}>
              <Form layout="inline" onFinish={(vals) => assignJudge(vals, () => judgeForm.resetFields())} form={judgeForm} initialValues={{ assignment_type: 'NORMAL' }}>
                <Form.Item name="person_id" rules={[{ required: true, message: 'Chọn Giám Khảo' }]}>
                  <Select placeholder="-- Chọn Giám khảo --" style={{ width: 220 }} showSearch optionFilterProp="children">
                      {judges.map(p => <Option key={p.id} value={p.id}>{p.fullName || p.full_name || p.name}</Option>)}
                  </Select>
                </Form.Item>
                <Form.Item name="assignment_target" rules={[{ required: true, message: 'Chọn hạng mục chấm' }]}>
                  <Select placeholder="-- Chọn Hạng mục phân công --" style={{ width: 280 }}>
                      <Select.OptGroup label="Chấm Sơ loại (Theo Bảng đấu)">
                          {tracks.map(t => <Option key={`TRACK_${t.id}`} value={`TRACK_${t.id}`}>{t.name}</Option>)}
                      </Select.OptGroup>
                      <Select.OptGroup label="Chấm Chung kết (Toàn Vòng)">
                          {rounds.filter(checkIsFinalRound).map(r => (
                              <Option key={`ROUND_${r.id}`} value={`ROUND_${r.id}`}>{r.name} (Vòng Chung kết)</Option>
                          ))}
                      </Select.OptGroup>
                  </Select>
                </Form.Item>
                <Form.Item name="assignment_type">
                  <Select style={{ width: 140 }}>
                      <Option value="NORMAL">Bình thường</Option>
                      <Option value="HEAD">Trưởng ban</Option>
                      <Option value="CALIBRATION">Chấm chéo</Option>
                  </Select>
                </Form.Item>
                <Button type="primary" htmlType="submit" loading={isLoading}>Phân công</Button>
              </Form>
            </Card>
            <Alert message="Hệ thống được bảo vệ bởi Database Triggers: Sẽ báo lỗi nếu bạn cố tình phân công Mentor làm Giám khảo tại chính Bảng đấu mà họ hướng dẫn." type="warning" showIcon style={{ marginBottom: 16 }} />
            
            <Table dataSource={judgeAssignments} rowKey="id" pagination={{ pageSize: 10 }} loading={isLoading} locale={{ emptyText: 'Chưa có Giám khảo nào được phân công' }} columns={[
              { 
                title: 'Tên Giám khảo', 
                render: (_, r) => {
                  // Ưu tiên quét ID trong danh sách Giám khảo gốc để lấy tên chuẩn xác nhất
                  const found = judges.find(j => j.id === r.person_id) || tempJudges.find(j => j.id === r.person_id);
                  const displayName = found ? (found.fullName || found.name) : r.judge_name;
                  return <strong>{displayName || 'Không xác định'}</strong>;
                } 
              },
              { title: 'Hạng mục chấm', dataIndex: 'target_name', render: t => <Tag color={t.includes('Vòng') ? 'gold' : 'blue'}>{t}</Tag> },
              { title: 'Vai trò', dataIndex: 'assignment_type', render: t => renderJudgeRole(t) },
              { title: 'Thao tác', width: 100, align: 'right', render: (_, r) => (
                <Popconfirm title="Gỡ quyền Giám khảo?" onConfirm={() => removeJudge(r.id)}>
                  <Button type="text" danger icon={<Trash2 size={16}/>} />
                </Popconfirm>
              )}
            ]} />
          </Tabs.TabPane>

        </Tabs>
      </Card>

      {/* Modal Mời Khách */}
      <Modal title="Mời Giám khảo khách mời" open={isModalOpen} onCancel={() => setIsModalOpen(false)} onOk={() => inviteForm.submit()} confirmLoading={isLoading} okText="Gửi lời mời">
        <Form form={inviteForm} layout="vertical" onFinish={(vals) => createTempJudge(vals, () => { setIsModalOpen(false); inviteForm.resetFields(); })}>
          <Form.Item name="fullName" label="Họ và Tên" rules={[{ required: true, message: 'Bắt buộc nhập' }]}><Input placeholder="VD: Nguyễn Văn A" /></Form.Item>
          <Form.Item name="email" label="Email" rules={[{ required: true, type: 'email', message: 'Email không hợp lệ' }]}><Input placeholder="VD: email@example.com" /></Form.Item>
          <Form.Item name="institution" label="Đơn vị/Tổ chức" rules={[{ required: true, message: 'Bắt buộc nhập' }]}><Input placeholder="Tên công ty hoặc trường đại học" /></Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default PeopleManagementPage;