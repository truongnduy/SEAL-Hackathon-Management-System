// src/features/people/pages/PeopleManagementPage.jsx
import React, { useState } from 'react';
import { Card, Tabs, Button, Table, Form, Input, Modal, Select, Tag, Popconfirm, Alert, Typography, message } from 'antd';
import { UserPlus, Trash2 } from 'lucide-react';
import { usePeopleManagement } from '../hooks/usePeopleManagement';
import {
  resolveFinalAssignmentType,
  resolvePrelimAssignmentType,
} from '../utils/peoplePersonnelRules';

const { Option } = Select;
const { Text } = Typography;

const PeopleManagementPage = ({ hackathonId }) => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [inviteForm] = Form.useForm();
  const [mentorForm] = Form.useForm();
  const [prelimJudgeForm] = Form.useForm();
  const [finalJudgeForm] = Form.useForm();

  const selectedMentorTrackId = Form.useWatch('track_id', mentorForm);
  const selectedPrelimPersonId = Form.useWatch('person_id', prelimJudgeForm);
  const selectedFinalPersonId = Form.useWatch('person_id', finalJudgeForm);

  const selectedPrelimJudgeTrackId = Form.useWatch('track_id', prelimJudgeForm);
  const selectedFinalJudgeTrackId = Form.useWatch('track_id', finalJudgeForm);

  const {
    mentors,
    judges,
    tempJudges,
    tracks,
    rounds,
    trackMentors,
    judgeAssignments,
    finalJudgeAssignments,
    prelimJudgePool,
    finalJudgePool,
    isLoading,
    createTempJudge,
    assignMentor,
    removeMentor,
    assignJudge,
    removeJudge,
    isMentorBlockedForTrack,
    isJudgeBlockedForTrack,
  } = usePeopleManagement(hackathonId);
  const trackByRoundType = (isFinal) =>
    tracks.filter((track) => {
      const roundId = track.roundId || track.round_id;
      const round = rounds.find((r) => r.id === roundId);
      const roundIsFinal =
        Boolean(round?.isFinal ?? round?.is_final) ||
        String(round?.roundType || round?.round_type || '').toUpperCase() === 'FINAL' ||
        /chung\s*kết|final/i.test(String(round?.name || ''));
      return roundIsFinal === isFinal;
    });
  const prelimTracks = trackByRoundType(false);
  const finalTracks = trackByRoundType(true);
  const finalRounds = rounds.filter((round) =>
    Boolean(round?.isFinal ?? round?.is_final) ||
    String(round?.roundType || round?.round_type || '').toUpperCase() === 'FINAL' ||
    /chung\s*kết|final/i.test(String(round?.name || ''))
  );

  const renderJudgeRole = (role) => {
    switch (role) {
      case 'HEAD':
        return <Tag color="red">Trưởng ban (HEAD)</Tag>;
      case 'FINAL_EXTERNAL':
        return <Tag color="purple">Giám khảo CK (FINAL_EXTERNAL)</Tag>;
      case 'CALIBRATION':
        return <Tag color="gold">Chấm chéo</Tag>;
      case 'NORMAL':
        return <Tag color="blue">Giám khảo (NORMAL)</Tag>;
      default:
        return <Tag color="blue">Giám khảo</Tag>;
    }
  };

  const mentorOptionsForTrack = (trackId) =>
    mentors
      .filter((m) => m.role === 'MENTOR')
      .map((p) => {
        const blocked = trackId && isMentorBlockedForTrack(p.id, trackId);
        return (
          <Option key={p.id} value={p.id} disabled={blocked}>
            {p.fullName || p.full_name || p.name}
            {blocked ? ' (đang là giám khảo cùng bảng)' : ''}
          </Option>
        );
      });

  const prelimJudgeOptionsForTrack = (trackId) =>
    prelimJudgePool.map((p) => {
      const blocked = trackId && isJudgeBlockedForTrack(p.id, trackId);
      const roleLabel = resolvePrelimAssignmentType(p);
      return (
        <Option key={p.id} value={p.id} disabled={blocked}>
          {p.fullName || p.full_name || p.name}
          {blocked ? ' (đang là mentor cùng bảng)' : ` — ${roleLabel}`}
        </Option>
      );
    });

  const finalJudgeOptionsForTrack = (trackId) =>
    finalJudgePool.map((p) => {
      const blocked = trackId && isJudgeBlockedForTrack(p.id, trackId);
      const roleLabel = resolveFinalAssignmentType(p);
      return (
        <Option key={p.id} value={p.id} disabled={blocked}>
          {p.fullName || p.full_name || p.name}
          {blocked ? ' (đang là mentor cùng bảng)' : ` — ${roleLabel}`}
        </Option>
      );
    });

  const selectedPrelimPerson = prelimJudgePool.find((p) => p.id === selectedPrelimPersonId);
  const selectedFinalPerson = finalJudgePool.find((p) => p.id === selectedFinalPersonId);
  const prelimRolePreview = selectedPrelimPerson
    ? resolvePrelimAssignmentType(selectedPrelimPerson)
    : null;
  const finalRolePreview = selectedFinalPerson
    ? resolveFinalAssignmentType(selectedFinalPerson)
    : null;

  return (
    <div>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16, borderRadius: 12 }}
        message="Phân công nhân sự theo bảng đấu"
        description={
          <Text type="secondary" style={{ fontSize: 13 }}>
            Ở giai đoạn chuẩn bị, bạn gán mentor và giám khảo cho từng <strong>bảng đấu</strong> — không cần chọn từng đội.
            Sau khi bốc thăm, mọi đội thuộc bảng nào sẽ được hỗ trợ/chấm bởi người đã gán cho bảng đó.
            Một người không thể vừa mentor vừa giám khảo cùng một bảng.
          </Text>
        }
      />

      <Card style={{ borderRadius: 12 }}>
        <Tabs defaultActiveKey="2" destroyInactiveTabPane>
          <Tabs.TabPane tab="Giám khảo khách mời" key="1">
            <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'flex-end' }}>
              <Button type="primary" icon={<UserPlus size={16} />} onClick={() => setIsModalOpen(true)}>
                Mời giám khảo
              </Button>
            </div>
            <Alert
              message="Giám khảo từ bên ngoài"
              description="Hệ thống gửi email kèm mật khẩu tạm (72 giờ) để họ đăng nhập."
              type="info"
              showIcon
              style={{ marginBottom: 16 }}
            />
            <Table
              dataSource={tempJudges}
              rowKey="id"
              pagination={false}
              loading={isLoading}
              locale={{ emptyText: 'Chưa mời giám khảo khách mời nào.' }}
              columns={[
                { title: 'Họ tên', dataIndex: 'fullName', render: (t, r) => <strong>{t || r.name}</strong> },
                { title: 'Email', dataIndex: 'email' },
                { title: 'Đơn vị', dataIndex: 'institution' },
                {
                  title: 'Trạng thái',
                  dataIndex: 'status',
                  render: (t) => <Tag color={t === 'APPROVED' ? 'green' : 'orange'}>{t === 'APPROVED' ? 'Đã duyệt' : 'Chờ duyệt'}</Tag>,
                },
              ]}
            />
          </Tabs.TabPane>

          <Tabs.TabPane tab="Mentor theo bảng đấu" key="2">
            <Card type="inner" style={{ marginBottom: 24, background: '#fafafa', borderRadius: 8 }}>
              <Form
                layout="inline"
                form={mentorForm}
                onFinish={(vals) => {
                  if (!tracks.length) {
                    return message.warning('Hãy tạo bảng đấu ở tab Bảng đấu trước.');
                  }
                  assignMentor(vals, () => mentorForm.resetFields());
                }}
              >
                <Form.Item
                  name="track_id"
                  rules={[{ required: true, message: 'Chọn bảng đấu' }]}
                  extra={
                    selectedMentorTrackId ? (
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        Mentor này sẽ hỗ trợ mọi đội thuộc bảng đã chọn sau khi bốc thăm.
                      </Text>
                    ) : null
                  }
                >
                  <Select placeholder="Chọn bảng đấu" style={{ width: 240 }} showSearch optionFilterProp="children">
                    {tracks.map((t) => (
                      <Option key={t.id} value={t.id}>
                        {t.name}
                      </Option>
                    ))}
                  </Select>
                </Form.Item>

                <Form.Item name="mentor_id" rules={[{ required: true, message: 'Chọn mentor' }]}>
                  <Select
                    placeholder="Chọn mentor"
                    style={{ width: 260 }}
                    showSearch
                    optionFilterProp="children"
                    disabled={!selectedMentorTrackId}
                  >
                    {mentorOptionsForTrack(selectedMentorTrackId)}
                  </Select>
                </Form.Item>

                <Button type="primary" htmlType="submit" loading={isLoading} disabled={!tracks.length}>
                  Gán mentor
                </Button>
              </Form>
            </Card>

            <Table
              dataSource={trackMentors}
              rowKey="id"
              pagination={false}
              loading={isLoading}
              locale={{ emptyText: 'Chưa gán mentor cho bảng đấu nào.' }}
              columns={[
                { title: 'Bảng đấu', dataIndex: 'track_name', render: (t) => <Tag color="blue">{t}</Tag> },
                {
                  title: 'Mentor',
                  render: (_, r) => {
                    const found = mentors.find((m) => m.id === r.mentor_id);
                    const name =
                      r.mentor_name ||
                      found?.fullName ||
                      found?.full_name ||
                      found?.name ||
                      'Không rõ';
                    return (
                      <Text strong style={{ color: '#1677ff' }}>
                        {name}
                      </Text>
                    );
                  },
                },
                {
                  title: '',
                  width: 80,
                  align: 'right',
                  render: (_, r) => (
                    <Popconfirm title="Gỡ mentor khỏi bảng này?" onConfirm={() => removeMentor(r.id)}>
                      <Button type="text" danger icon={<Trash2 size={16} />} />
                    </Popconfirm>
                  ),
                },
              ]}
            />
          </Tabs.TabPane>

          <Tabs.TabPane tab="Giám khảo Sơ loại" key="3">
            <Card type="inner" style={{ marginBottom: 24, background: '#fafafa', borderRadius: 8 }}>
              <Form
                layout="inline"
                form={prelimJudgeForm}
                onFinish={(vals) =>
                  assignJudge({ ...vals, is_final_assignment: false }, () =>
                    prelimJudgeForm.resetFields(['person_id', 'track_id'])
                  )
                }
              >
                <Form.Item name="track_id" rules={[{ required: true, message: 'Chọn bảng đấu' }]}>
                  <Select placeholder="Chọn bảng đấu" style={{ width: 220 }} showSearch optionFilterProp="children">
                    {prelimTracks.map((t) => (
                      <Option key={t.id} value={t.id}>
                        {t.name}
                      </Option>
                    ))}
                  </Select>
                </Form.Item>

                <Form.Item name="person_id" rules={[{ required: true, message: 'Chọn giám khảo' }]}>
                  <Select
                    placeholder="Chọn giám khảo"
                    style={{ width: 220 }}
                    showSearch
                    optionFilterProp="children"
                    disabled={!selectedPrelimJudgeTrackId}
                  >
                    {prelimJudgeOptionsForTrack(selectedPrelimJudgeTrackId)}
                  </Select>
                </Form.Item>

                <Form.Item label="Vai trò">
                  {prelimRolePreview ? (
                    renderJudgeRole(prelimRolePreview)
                  ) : (
                    <Text type="secondary">Chọn giám khảo để hiện vai trò</Text>
                  )}
                </Form.Item>

                <Button type="primary" htmlType="submit" loading={isLoading} disabled={!prelimTracks.length}>
                  Gán giám khảo
                </Button>
              </Form>
              <Text type="secondary" style={{ fontSize: 12, display: 'block', marginTop: 12 }}>
                Chỉ hiện giám khảo/mentor <strong>INTERNAL</strong> hoặc <strong>trưởng ban</strong>. Vai trò gán tự lấy từ hồ sơ (HEAD / NORMAL).
              </Text>
            </Card>

            <Table
              dataSource={judgeAssignments}
              rowKey="id"
              pagination={{ pageSize: 10 }}
              loading={isLoading}
              locale={{ emptyText: 'Chưa gán giám khảo nào.' }}
              columns={[
                {
                  title: 'Giám khảo',
                  render: (_, r) => {
                    const found = judges.find((j) => j.id === r.person_id) || tempJudges.find((j) => j.id === r.person_id);
                    return <strong>{found?.fullName || found?.name || r.judge_name || 'Không rõ'}</strong>;
                  },
                },
                { title: 'Bảng đấu', dataIndex: 'target_name', render: (t) => <Tag color="geekblue">{t}</Tag> },
                { title: 'Vai trò', dataIndex: 'assignment_type', render: renderJudgeRole },
                {
                  title: '',
                  width: 80,
                  align: 'right',
                  render: (_, r) => (
                    <Popconfirm title="Gỡ giám khảo khỏi bảng này?" onConfirm={() => removeJudge(r.id)}>
                      <Button type="text" danger icon={<Trash2 size={16} />} />
                    </Popconfirm>
                  ),
                },
              ]}
            />
          </Tabs.TabPane>

          <Tabs.TabPane tab="Giám khảo Chung kết" key="4">
            <Alert
              type="info"
              showIcon
              style={{ marginBottom: 16 }}
              message="Bước 4.5 - Gán giám khảo Chung kết"
              description="Luồng này dùng API round-scoped theo BE: POST /api/v1/rounds/{finalRoundId}/judge-assignments."
            />
            <Card type="inner" style={{ marginBottom: 24, background: '#fafafa', borderRadius: 8 }}>
              <Form
                layout="inline"
                form={finalJudgeForm}
                initialValues={{ is_final_assignment: true }}
                onFinish={(vals) =>
                  assignJudge(vals, () =>
                    finalJudgeForm.resetFields(['person_id', 'track_id', 'round_id'])
                  )
                }
              >
                {finalTracks.length > 0 ? (
                  <Form.Item name="track_id" rules={[{ required: true, message: 'Chọn bảng đấu CK' }]}>
                    <Select placeholder="Chọn bảng đấu CK" style={{ width: 240 }} showSearch optionFilterProp="children">
                      {finalTracks.map((t) => (
                        <Option key={t.id} value={t.id}>
                          {t.name}
                        </Option>
                      ))}
                    </Select>
                  </Form.Item>
                ) : (
                  <Form.Item name="round_id" rules={[{ required: true, message: 'Chọn vòng CK' }]}>
                    <Select placeholder="Chọn vòng Chung kết" style={{ width: 240 }} showSearch optionFilterProp="children">
                      {finalRounds.map((r) => (
                        <Option key={r.id} value={r.id}>
                          {r.name}
                        </Option>
                      ))}
                    </Select>
                  </Form.Item>
                )}
                <Form.Item name="person_id" rules={[{ required: true, message: 'Chọn giám khảo' }]}>
                  <Select
                    placeholder="Chọn giám khảo"
                    style={{ width: 240 }}
                    showSearch
                    optionFilterProp="children"
                    disabled={!selectedFinalJudgeTrackId && finalTracks.length > 0}
                  >
                    {finalJudgeOptionsForTrack(selectedFinalJudgeTrackId)}
                  </Select>
                </Form.Item>
                <Form.Item name="is_final_assignment" hidden>
                  <Input />
                </Form.Item>
                <Form.Item label="Vai trò">
                  {finalRolePreview ? (
                    renderJudgeRole(finalRolePreview)
                  ) : (
                    <Text type="secondary">Chọn giám khảo để hiện vai trò</Text>
                  )}
                </Form.Item>
                <Button type="primary" htmlType="submit" loading={isLoading} disabled={!finalTracks.length && !finalRounds.length}>
                  Gán giám khảo CK
                </Button>
              </Form>
              <Text type="secondary" style={{ fontSize: 12, display: 'block', marginTop: 12 }}>
                Chỉ hiện giám khảo <strong>EXTERNAL</strong> hoặc <strong>trưởng ban</strong>. Mentor và INTERNAL thường không được chấm Chung kết. Vai trò tự lấy từ hồ sơ.
              </Text>
            </Card>
            <Table
              dataSource={finalJudgeAssignments}
              rowKey="id"
              pagination={{ pageSize: 10 }}
              loading={isLoading}
              locale={{ emptyText: 'Chưa gán giám khảo Chung kết.' }}
              columns={[
                {
                  title: 'Giám khảo',
                  render: (_, r) => {
                    const found = judges.find((j) => j.id === r.person_id) || tempJudges.find((j) => j.id === r.person_id);
                    return <strong>{found?.fullName || found?.name || r.judge_name || 'Không rõ'}</strong>;
                  },
                },
                { title: 'Vòng', dataIndex: 'target_name', render: (t) => <Tag color="purple">{t}</Tag> },
                { title: 'Vai trò', dataIndex: 'assignment_type', render: renderJudgeRole },
              ]}
            />
          </Tabs.TabPane>
        </Tabs>
      </Card>

      <Modal
        title="Mời giám khảo khách mời"
        open={isModalOpen}
        onCancel={() => setIsModalOpen(false)}
        onOk={() => inviteForm.submit()}
        confirmLoading={isLoading}
        okText="Gửi lời mời"
      >
        <Form
          form={inviteForm}
          layout="vertical"
          onFinish={(vals) =>
            createTempJudge(vals, () => {
              setIsModalOpen(false);
              inviteForm.resetFields();
            })
          }
        >
          <Form.Item name="fullName" label="Họ và tên" rules={[{ required: true, message: 'Nhập họ tên' }]}>
            <Input placeholder="Ví dụ: Nguyễn Văn A" />
          </Form.Item>
          <Form.Item name="email" label="Email" rules={[{ required: true, type: 'email', message: 'Email không hợp lệ' }]}>
            <Input placeholder="email@congty.com" />
          </Form.Item>
          <Form.Item name="institution" label="Đơn vị / tổ chức" rules={[{ required: true, message: 'Nhập đơn vị' }]}>
            <Input placeholder="Tên công ty hoặc trường" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default PeopleManagementPage;
