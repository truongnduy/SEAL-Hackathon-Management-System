import React from 'react';
import { Card, Result } from 'antd';

const LateSubmissionReviewPage: React.FC = () => {
  return (
    <div style={{ padding: '24px' }}>
      <Card>
        <Result
          status="info"
          title="Trang này đã ngừng hoạt động"
        />
      </Card>
    </div>
  );
};

export default LateSubmissionReviewPage;
