import React from 'react';
import { Card, Button, Form, message, Space } from 'antd';
import { useNavigate } from 'react-router-dom';
import PageHeader from '../../../shared/components/ui/PageHeader';
import HackathonForm from '../components/HackathonForm';
import { ROUTES } from '../../../shared/constants/routes';
import { hackathonService } from '../services/hackathonService';
import { mapHackathonToBE } from '../mappers/hackathonMapper';
import dayjs from 'dayjs';

const CreateHackathonPage = () => {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const [loading, setLoading] = React.useState(false);

  const handleFinish = async (values) => {
    try {
      setLoading(true);
      const payload = mapHackathonToBE(values);
      await hackathonService.create(payload);
      message.success('Đã tạo sự kiện thành công');
      navigate(ROUTES.HACKATHONS);
    } catch (error) {
      message.error(error.message || 'Lỗi khi tạo sự kiện');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <PageHeader 
        title="Tạo Sự kiện Mới" 
        subtitle="Thiết lập các thông tin cơ bản cho sự kiện hackathon của bạn"
        onBack={() => navigate(ROUTES.HACKATHONS)}
      />
      
      <Card style={{ borderRadius: 12 }}>
        <HackathonForm form={form} onFinish={handleFinish} />
        
        <div style={{ marginTop: 24, display: 'flex', justifyContent: 'flex-end' }}>
          <Space>
            <Button onClick={() => navigate(ROUTES.HACKATHONS)} disabled={loading}>Hủy</Button>
            <Button type="primary" onClick={() => form.submit()} size="large" loading={loading}>
              Tạo Sự kiện
            </Button>
          </Space>
        </div>
      </Card>
    </div>
  );
};

export default CreateHackathonPage;
