import React, { useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Row, Col, Select, DatePicker, Switch, Tooltip } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';

const { Option } = Select;

const RoundFormModal = ({ visible, onCancel, onFinish, initialValues, title, existingRounds = [], hackathon }) => {
  const [form] = Form.useForm();
  const isFinal = Form.useWatch('is_final', form);
  const hasPrelimRound = existingRounds.some((r) => !r.is_final);

  // Tập hợp round_type đã dùng (loại trừ round đang edit)
  const usedRoundTypes = new Set(
    existingRounds
      .filter((r) => r.id !== initialValues?.id)
      .map((r) => r.round_type)
      .filter(Boolean)
  );

  useEffect(() => {
    if (visible) {
      if (initialValues) {
        form.setFieldsValue({
          ...initialValues,
          exam_at: initialValues.exam_at ? dayjs(initialValues.exam_at) : null,
          submission_open: initialValues.submission_open ? dayjs(initialValues.submission_open) : null,
          submission_deadline: initialValues.submission_deadline ? dayjs(initialValues.submission_deadline) : null,
        });
      } else {
        form.resetFields();
      }
    }
  }, [visible, initialValues, form]);

  const prelimRound = existingRounds.find(r => !r.is_final && r.id !== initialValues?.id);
  const finalRound = existingRounds.find(r => r.is_final && r.id !== initialValues?.id);

  const getDisabledDate = (current) => {
    if (!current) return false;
    
    let minDate = dayjs().startOf('day');
    if (hackathon && hackathon.registration_end) {
      const regEndPlus4 = dayjs(hackathon.registration_end).add(5, 'day').startOf('day');
      if (regEndPlus4.isAfter(minDate)) {
        minDate = regEndPlus4;
      }
    }
    if (current.isBefore(minDate)) return true;

    // Khoá chéo: Vòng Chung kết phải diễn ra sau Sơ loại
    if (isFinal && prelimRound?.exam_at) {
      if (current.isBefore(dayjs(prelimRound.exam_at).startOf('day'))) return true;
    }

    // Khoá chéo: Vòng Sơ loại phải kết thúc trước Chung kết
    if (!isFinal && finalRound?.submission_open) {
      if (current.isAfter(dayjs(finalRound.submission_open).endOf('day'))) return true;
    }

    return false;
  };

  const getDisabledTime = (current) => {
    if (!current) return {};
    const now = dayjs();
    
    if (current.isSame(now, 'day')) {
      return {
        disabledHours: () => Array.from({ length: 24 }, (_, i) => i).filter(h => h < now.hour()),
        disabledMinutes: (selectedHour) => {
          if (selectedHour === now.hour()) {
            return Array.from({ length: 60 }, (_, i) => i).filter(m => m < now.minute());
          }
          return [];
        },
        disabledSeconds: (selectedHour, selectedMinute) => {
          if (selectedHour === now.hour() && selectedMinute === now.minute()) {
            return Array.from({ length: 60 }, (_, i) => i).filter(s => s < now.second());
          }
          return [];
        }
      };
    }
    return {};
  };

  const handleSubmit = () => {
    form.validateFields()
      .then(values => {
        const formattedValues = {
          ...values,
          exam_at: values.exam_at?.format('YYYY-MM-DD HH:mm:ss'),
          submission_open: values.submission_open?.format('YYYY-MM-DD HH:mm:ss'),
          submission_deadline: values.submission_deadline?.format('YYYY-MM-DD HH:mm:ss'),
        };
        onFinish(formattedValues);
        form.resetFields();
      })
      .catch(info => {
        console.log('Validate Failed:', info);
      });
  };

  return (
    <Modal
      open={visible}
      title={title}
      okText="Lưu"
      cancelText="Hủy"
      onCancel={onCancel}
      onOk={handleSubmit}
      width={800}
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          tiebreak_rule: 'PENALTY_SCORE',
          is_active: false,
          wildcard_enabled: false,
          is_final: false,
          round_type: 'PRELIMINARY'
        }}
        onValuesChange={(changedValues, allValues) => {
          if (changedValues.exam_at !== undefined || changedValues.coding_duration_hours !== undefined) {
            const examAt = allValues.exam_at;
            const duration = allValues.coding_duration_hours;
            if (examAt && duration) {
              // Dùng integer arithmetic (minutes) để khớp chính xác với BE:
              // openOffsetMinutes = Math.floor((duration * 60 * 2) / 3) — giống long division của Java
              const openOffsetMinutes = Math.floor((duration * 60 * 2) / 3);
              const submissionOpen = dayjs(examAt).add(openOffsetMinutes, 'minute');
              const submissionDeadline = dayjs(examAt).add(duration, 'hour');
              form.setFieldsValue({
                submission_open: submissionOpen,
                submission_deadline: submissionDeadline,
              });
            }
          }
        }}
      >
        <Form.Item
          name="name"
          label="Tên vòng thi"
          rules={[{ required: true, message: 'Vui lòng nhập tên vòng thi' }]}
        >
          <Input placeholder="Ví dụ: Vòng Sơ loại" />
        </Form.Item>

        <Row gutter={24}>
          <Col span={12}>
            <Form.Item
              name="round_type"
              label="Loại vòng thi"
              rules={[{ required: true, message: 'Vui lòng chọn loại' }]}
            >
              <Select
                onChange={(value) => {
                  form.setFieldsValue({
                    is_final: value === 'FINAL',
                  });
                }}
              >
                <Option value="PRELIMINARY" disabled={usedRoundTypes.has('PRELIMINARY')}>
                  Sơ loại (Preliminary){usedRoundTypes.has('PRELIMINARY') ? ' — đã tạo' : ''}
                </Option>
                <Option value="SEMIFINAL" disabled={usedRoundTypes.has('SEMIFINAL')}>
                  Bán kết (Semifinal){usedRoundTypes.has('SEMIFINAL') ? ' — đã tạo' : ''}
                </Option>
                <Option value="FINAL" disabled={usedRoundTypes.has('FINAL')}>
                  Chung kết (Final){usedRoundTypes.has('FINAL') ? ' — đã tạo' : ''}
                </Option>
              </Select>
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="is_final"
              label="Là vòng chung kết"
              valuePropName="checked"
              tooltip={!hasPrelimRound && !initialValues ? 'Tạo vòng Sơ loại trước khi tạo Chung kết' : undefined}
            >
              <Switch
                onChange={(checked) => {
                  if (checked) {
                    form.setFieldsValue({ round_type: 'FINAL', is_final: true });
                  } else {
                    const currentType = form.getFieldValue('round_type');
                    form.setFieldsValue({
                      is_final: false,
                      round_type: currentType === 'FINAL' ? 'PRELIMINARY' : currentType,
                    });
                  }
                }}
              />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={24}>
          <Col span={12}>
            <Form.Item
              name="exam_at"
              label={
                <span>
                  Ngày giờ thi{' '}
                  <Tooltip title="Thời điểm thi đấu / trình bày — khác với hạn chót nộp bài">
                    <InfoCircleOutlined style={{ color: '#8c8c8c' }} />
                  </Tooltip>
                </span>
              }
              dependencies={['submission_open']}
              validateTrigger={['onChange', 'onBlur']}
              rules={[
                { required: true, message: 'Vui lòng chọn ngày giờ thi' },
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    const open = getFieldValue('submission_open');
                    if (!value || !open || dayjs(value).isBefore(dayjs(open))) {
                      return Promise.resolve();
                    }
                    return Promise.reject(
                      new Error('Ngày giờ thi phải trước thời điểm mở nộp bài')
                    );
                  },
                }),
              ]}
            >
              <DatePicker 
                showTime 
                style={{ width: '100%' }} 
                disabledDate={getDisabledDate}
                disabledTime={getDisabledTime}
              />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="coding_duration_hours"
              label="Thời gian thi (Giờ)"
              validateTrigger={['onChange', 'onBlur']}
              rules={[
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    if (value === undefined || value === null || value > 0) {
                      return Promise.resolve();
                    }
                    return Promise.reject(new Error('Thời lượng phải > 0'));
                  },
                }),
              ]}
            >
              <InputNumber min={1} step={1} precision={0} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={24}>
          <Col span={12}>
            <Form.Item
              name="submission_open"
              label="Mở nộp bài"
              dependencies={['exam_at', 'submission_deadline']}
              validateTrigger={['onChange', 'onBlur']}
              rules={[
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    const examAt = getFieldValue('exam_at');
                    if (!value || !examAt || dayjs(value).isAfter(dayjs(examAt))) {
                      return Promise.resolve();
                    }
                    return Promise.reject(
                      new Error('Thời điểm mở nộp bài phải sau ngày giờ thi')
                    );
                  },
                }),
              ]}
            >
              <DatePicker
                showTime
                style={{ width: '100%' }}
                disabledDate={getDisabledDate}
                disabledTime={getDisabledTime}
              />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="submission_deadline"
              label="Hạn chót nộp bài"
              dependencies={['submission_open']}
              validateTrigger={['onChange', 'onBlur']}
              rules={[
                { required: true, message: 'Bắt buộc' },
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    const open = getFieldValue('submission_open');
                    if (!value || !open || dayjs(value).isAfter(dayjs(open))) {
                      return Promise.resolve();
                    }
                    return Promise.reject(new Error('Hạn chót phải sau thời gian mở nộp bài'));
                  },
                }),
              ]}
            >
              <DatePicker
                showTime
                style={{ width: '100%' }}
                disabledDate={getDisabledDate}
                disabledTime={getDisabledTime}
              />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={24}>
          <Col span={12}>
            <Form.Item
              name="problem_statement_url"
              label="Link Đề bài"
            >
              <Input placeholder="https://example.com/problem" />
            </Form.Item>
          </Col>
        </Row>

        {!isFinal && (
          <Row gutter={24}>
            <Col span={8}>
              <Form.Item
                name="top_n_advance"
                label="Số đội đi tiếp (Top N)"
              >
                <InputNumber min={1} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="min_teams_final"
                label="Số đội tối thiểu vào Chung kết"
              >
                <InputNumber min={1} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="tiebreak_rule"
                label="Luật Tiebreak"
              >
                <Select>
                  <Option value="PENALTY_SCORE">Điểm phạt (Penalty)</Option>
                  <Option value="LATEST_SUBMISSION">Bài nộp muộn nhất</Option>
                  <Option value="EARLIEST_SUBMISSION">Bài nộp sớm nhất</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
        )}

        {isFinal && (
          <Row gutter={24}>
            <Col span={12}>
              <Form.Item
                name="tiebreak_rule"
                label="Luật Tiebreak"
              >
                <Select>
                  <Option value="PENALTY_SCORE">Điểm phạt (Penalty)</Option>
                  <Option value="LATEST_SUBMISSION">Bài nộp muộn nhất</Option>
                  <Option value="EARLIEST_SUBMISSION">Bài nộp sớm nhất</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
        )}

        <Row gutter={24}>
          <Col span={12}>
            <Form.Item
              name="wildcard_enabled"
              label="Cho phép Wildcard"
              valuePropName="checked"
            >
              <Switch />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="is_active"
              label="Đang hoạt động"
              valuePropName="checked"
            >
              <Switch disabled={!!initialValues?.is_active} />
            </Form.Item>
          </Col>
        </Row>
      </Form>
    </Modal>
  );
};

export default RoundFormModal;
