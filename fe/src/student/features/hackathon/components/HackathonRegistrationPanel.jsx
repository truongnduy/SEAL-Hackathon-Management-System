import { Badge, Button, Card, Empty, Modal, Space, Spin, Tag, Typography, message, theme } from 'antd';

import { useStudentHackathonRegistration } from '../hooks/useStudentHackathonRegistration';

import { getStudentHackathonErrorMessage } from '../constants/studentHackathon.constants';



const { Text, Title } = Typography;



const HackathonRegistrationPanel = ({ hasTeam = false, onRegistrationChange }) => {

  const { token } = theme.useToken();

  const {

    hackathons,

    loading,

    actionLoading,

    registrationBlocked,

    register,

    unregister,

  } = useStudentHackathonRegistration();



  const handleRegister = async (hackathonId, hackathonName) => {

    Modal.confirm({

      title: 'Xác nhận đăng ký tham gia?',

      content: (

        <>

          Bạn sẽ đăng ký tham gia <strong>{hackathonName}</strong>.

          Mỗi người chỉ được đăng ký một giải tại một thời điểm và không thể đăng ký lại sau khi hủy.

        </>

      ),

      okText: 'Đăng ký',

      cancelText: 'Hủy',

      onOk: async () => {

        const result = await register(hackathonId);

        if (result.success) {

          message.success('Đăng ký tham gia hackathon thành công');

          onRegistrationChange?.();

          return;

        }

        message.error(getStudentHackathonErrorMessage(result.error));

      },

    });

  };



  const handleUnregister = async (hackathonId, hackathonName) => {

    Modal.confirm({

      title: 'Xác nhận hủy đăng ký?',

      content: (

        <>

          Bạn sẽ hủy đăng ký <strong>{hackathonName}</strong>.

          Mỗi người chỉ được hủy đăng ký một lần và không thể đăng ký lại giải này.

        </>

      ),

      okText: 'Hủy đăng ký',

      okButtonProps: { danger: true },

      cancelText: 'Đóng',

      onOk: async () => {

        const result = await unregister(hackathonId);

        if (result.success) {

          message.success('Đã hủy đăng ký hackathon');

          onRegistrationChange?.();

          return;

        }

        message.error(getStudentHackathonErrorMessage(result.error, 'Không thể hủy đăng ký'));

      },

    });

  };



  if (loading) {

    return (

      <Card style={{ borderRadius: 16, border: `1px solid ${token.colorBorderSecondary}` }}>

        <div style={{ textAlign: 'center', padding: 24 }}>

          <Spin />

        </div>

      </Card>

    );

  }



  if (!hackathons.length) {

    return null;

  }



  return (

    <Card

      title="Đăng ký Hackathon"

      style={{

        borderRadius: 16,

        border: `1px solid ${token.colorBorderSecondary}`,

        boxShadow: token.boxShadowTertiary,

      }}

    >

      <Space direction="vertical" size={12} style={{ width: '100%' }}>

        {hackathons.map((item) => {

          const isRegistered = Boolean(item.registered);

          const isSlotFull = registrationBlocked[item.id];

          const isWithdrawn = Boolean(item.registrationWithdrawn);

          const isRegisteredElsewhere = Boolean(item.registeredElsewhere);

          const canRegister = !isRegistered && !isSlotFull && !isWithdrawn && !isRegisteredElsewhere;



          return (

            <div

              key={item.id}

              style={{

                display: 'flex',

                flexWrap: 'wrap',

                gap: 12,

                alignItems: 'center',

                justifyContent: 'space-between',

                padding: '12px 14px',

                borderRadius: 12,

                border: `1px solid ${token.colorBorderSecondary}`,

                background: token.colorBgContainer,

              }}

            >

              <div style={{ minWidth: 0, flex: 1 }}>

                <Title level={5} style={{ margin: 0 }}>

                  {item.name}

                </Title>

                <Space size={8} wrap style={{ marginTop: 6 }}>

                  <Tag color="processing">{item.status}</Tag>

                  {isRegistered ? (

                    <Badge status="success" text="Đã đăng ký" />

                  ) : isWithdrawn ? (

                    <Badge status="error" text="Đã hủy đăng ký" />

                  ) : (

                    <Badge status="default" text="Chưa đăng ký" />

                  )}

                </Space>

                {isSlotFull && (

                  <Text type="danger" style={{ display: 'block', marginTop: 8, fontSize: 13 }}>

                    Đăng ký thất bại: Giải đấu đã đạt giới hạn tối đa số lượng người tham gia.

                  </Text>

                )}

                {isWithdrawn && (

                  <Text type="secondary" style={{ display: 'block', marginTop: 8, fontSize: 13 }}>

                    Bạn đã hủy đăng ký giải này và không thể đăng ký lại.

                  </Text>

                )}

                {isRegisteredElsewhere && (

                  <Text type="secondary" style={{ display: 'block', marginTop: 8, fontSize: 13 }}>

                    Bạn đã đăng ký một giải khác. Mỗi người chỉ được đăng ký một giải tại một thời điểm.

                  </Text>

                )}

              </div>



              <Space>

                {canRegister && (

                  <Button

                    type="primary"

                    loading={actionLoading}

                    onClick={() => handleRegister(item.id, item.name)}

                  >

                    Đăng ký tham gia

                  </Button>

                )}

                {isRegistered && !hasTeam && (

                  <Button

                    danger

                    loading={actionLoading}

                    onClick={() => handleUnregister(item.id, item.name)}

                  >

                    Hủy đăng ký

                  </Button>

                )}

              </Space>

            </div>

          );

        })}

      </Space>



      {!hackathons.length && <Empty description="Không có hackathon đang mở đăng ký" />}

    </Card>

  );

};



export default HackathonRegistrationPanel;

