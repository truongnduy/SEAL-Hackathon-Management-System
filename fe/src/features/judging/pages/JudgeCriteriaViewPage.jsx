import React, { useState, useEffect } from 'react';
import { Typography, Card, Select, Table, Tag, Alert, Spin, Space } from 'antd';
import { FileTextOutlined, InfoCircleOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';

const { Title, Text } = Typography;

const JudgeCriteriaViewPage = () => {
  const [loading, setLoading] = useState(true);
  const [selectedAssignment, setSelectedAssignment] = useState('101');
  const [criteriaData, setCriteriaData] = useState([]);

  // MOCK DATA: Giả lập danh sách nhiệm vụ và tiêu chí
  const mockAssignments = [
    { value: '101', label: 'Chung Kết - AI & Machine Learning' },
    { value: '102', label: 'Sơ Loại - Web Development' }
  ];

  const mockCriteria = {
    '101': [
      { id: 1, name: 'Xử lý & Truy xuất Dữ liệu', type: 'TECHNICAL', weight: 0.30, maxScore: 10, description: 'Đánh giá độ chính xác và tốc độ xử lý dữ liệu của mô hình.' },
      { id: 2, name: 'Độ tin cậy & Chống ảo giác', type: 'TECHNICAL', weight: 0.20, maxScore: 10, description: 'Mô hình không bịa đặt thông tin (hallucination).' },
      { id: 3, name: 'Tư duy Agent', type: 'TECHNICAL', weight: 0.20, maxScore: 10, description: 'Khả năng tự động hóa và xử lý đa tầng của hệ thống.' },
      { id: 4, name: 'Tính thực tế & Tối ưu', type: 'TECHNICAL', weight: 0.20, maxScore: 10, description: 'Khả năng áp dụng vào thực tế doanh nghiệp.' },
      { id: 5, name: 'Q&A & Phản biện', type: 'SOFT_SKILL', weight: 0.10, maxScore: 10, description: 'Kỹ năng trả lời câu hỏi và bảo vệ quan điểm trước Hội đồng.' },
    ],
    '102': [
      { id: 6, name: 'Kiến trúc phần mềm', type: 'TECHNICAL', weight: 0.40, maxScore: 10, description: 'Clean code, cấu trúc thư mục và design pattern.' },
      { id: 7, name: 'Giao diện & Trải nghiệm (UI/UX)', type: 'SOFT_SKILL', weight: 0.30, maxScore: 10, description: 'Tính thân thiện, dễ sử dụng và thẩm mỹ của web.' },
      { id: 8, name: 'Tính đổi mới & Sáng tạo', type: 'TECHNICAL', weight: 0.30, maxScore: 10, description: 'Sự khác biệt của sản phẩm so với các giải pháp hiện có.' },
    ]
  };

  useEffect(() => {
    // Giả lập load data từ API mất 0.5s
    setLoading(true);
    const timer = setTimeout(() => {
      setCriteriaData(mockCriteria[selectedAssignment] || []);
      setLoading(false);
    }, 500);
    return () => clearTimeout(timer);
  }, [selectedAssignment]);

  const columns = [
    {
      title: 'STT',
      key: 'index',
      width: 60,
      align: 'center',
      render: (text, record, index) => index + 1,
    },
    {
      title: 'Tên tiêu chí',
      dataIndex: 'name',
      key: 'name',
      render: (text, record) => (
        <div>
          <Text strong>{text}</Text>
          <div style={{ fontSize: '12px', color: '#8c8c8c', marginTop: '4px' }}>
            {record.description}
          </div>
        </div>
      )
    },
    {
      title: 'Loại',
      dataIndex: 'type',
      key: 'type',
      width: 120,
      render: (type) => (
        <Tag color={type === 'TECHNICAL' ? 'blue' : 'orange'} style={{ fontWeight: 600 }}>
          {type}
        </Tag>
      ),
    },
    {
      title: 'Trọng số',
      dataIndex: 'weight',
      key: 'weight',
      width: 100,
      align: 'center',
      render: (weight) => <Text strong style={{ color: '#1677ff' }}>{weight.toFixed(2)}</Text>,
    },
    {
      title: 'Điểm Max',
      dataIndex: 'maxScore',
      key: 'maxScore',
      width: 100,
      align: 'center',
      render: (score) => <Text strong>{score}</Text>,
    }
  ];

  const totalWeight = criteriaData.reduce((sum, item) => sum + item.weight, 0);

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }} style={{ maxWidth: 1200, margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: 24 }}>
        <FileTextOutlined style={{ fontSize: 24, color: '#1677ff', marginRight: 12 }} />
        <Title level={2} style={{ margin: 0 }}>Từ điển Tiêu chí Đánh giá</Title>
      </div>

      <Card style={{ borderRadius: 16, boxShadow: '0 4px 12px rgba(0,0,0,0.03)' }}>
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          
          <Alert
            message="Chế độ Xem (Read-only)"
            description="Là Giám khảo, bạn có thể xem trước các tiêu chí chấm điểm và trọng số của từng vòng thi. Các tiêu chí này được thiết lập bởi Ban tổ chức và không thể chỉnh sửa."
            type="info"
            showIcon
            icon={<InfoCircleOutlined />}
            style={{ borderRadius: 8 }}
          />

          <div>
            <Text strong style={{ display: 'block', marginBottom: 8 }}>Chọn Nhiệm vụ / Vòng thi:</Text>
            <Select
              size="large"
              value={selectedAssignment}
              onChange={setSelectedAssignment}
              options={mockAssignments}
              style={{ width: '100%', maxWidth: 400 }}
            />
          </div>

          <div style={{ background: totalWeight === 1 ? '#f6ffed' : '#fff2f0', border: `1px solid ${totalWeight === 1 ? '#b7eb8f' : '#ffccc7'}`, padding: '12px 16px', borderRadius: 8, display: 'inline-block' }}>
            <Text strong style={{ color: totalWeight === 1 ? '#389e0d' : '#cf1322' }}>
              Tổng trọng số hiện tại: {totalWeight.toFixed(2)} {totalWeight === 1 ? '(Hợp lệ)' : '(Chưa đạt 1.0)'}
            </Text>
          </div>

          <Spin spinning={loading} tip="Đang tải dữ liệu tiêu chí...">
            <Table
              columns={columns}
              dataSource={criteriaData}
              rowKey="id"
              pagination={false}
              bordered
              style={{ marginTop: 8 }}
            />
          </Spin>

        </Space>
      </Card>
    </motion.div>
  );
};

export default JudgeCriteriaViewPage;