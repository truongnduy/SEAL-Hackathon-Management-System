import { Button, Popconfirm, Space, Tooltip, Typography, theme } from "antd";
import {
  CheckOutlined,
  CloseOutlined,
  DeleteOutlined,
  MoreOutlined,
  WarningOutlined,
} from "@ant-design/icons";

const { Text } = Typography;

const TeamActionButtons = ({
  compact = false,
  disabledReason = "",
  canApprove,
  canReject = true,
  canDisband = true,
  loading,
  onApprove,
  onReject,
  onDisband,
}) => {
  const { token } = theme.useToken();
  const approveStyle = {
    background: token.colorSuccess,
    borderColor: token.colorSuccess,
    borderRadius: token.borderRadius,
    fontWeight: 600,
  };
  const hasAnyAction = canApprove || canReject || canDisband;

  if (!hasAnyAction) {
    return (
      <Text type="secondary" style={{ fontSize: 13 }}>
        Không có thao tác
      </Text>
    );
  }

  const disbandButton = (
    <Popconfirm
      title="Giải tán đội thi?"
      description="Chỉ thực hiện khi đội chưa có dữ liệu thi đấu bị ràng buộc."
      onConfirm={onDisband}
      okText="Giải tán"
      cancelText="Hủy"
      okButtonProps={{ danger: true }}
      disabled={!canDisband}
      icon={<WarningOutlined style={{ color: "#faad14" }} />}
    >
      <Tooltip
        title={
          canDisband
            ? "Giải tán đội"
            : "Chỉ đội PENDING hoặc ACTIVE mới có thể giải tán"
        }
      >
        <Button
          danger
          disabled={!canDisband}
          icon={compact ? <DeleteOutlined /> : <MoreOutlined />}
          loading={loading}
          type={compact ? "default" : "text"}
          aria-label="Giải tán đội"
          style={{
            borderRadius: 8,
            width: compact && !canApprove && !canReject ? "100%" : undefined,
          }}
        >
          {compact && !canApprove && !canReject ? "Giải tán" : null}
        </Button>
      </Tooltip>
    </Popconfirm>
  );

  if (compact) {
    return (
      <div
        style={{
          display: "grid",
          gap: 8,
          gridTemplateColumns: canApprove || canReject ? "1fr 1fr auto" : "1fr",
          marginTop: 12,
        }}
      >
        {canApprove && (
          <Tooltip title={disabledReason}>
            <Button
              block
              type="primary"
              icon={<CheckOutlined />}
              loading={loading}
              onClick={onApprove}
              style={approveStyle}
            >
              Duyệt
            </Button>
          </Tooltip>
        )}
        {canReject && (
          <Button
            block
            danger
            icon={<CloseOutlined />}
            loading={loading}
            onClick={onReject}
            style={{ borderRadius: 8 }}
          >
            Từ chối
          </Button>
        )}
        {canDisband && disbandButton}
      </div>
    );
  }

  return (
    <Space size="small">
      {canApprove && (
        <Tooltip title={disabledReason}>
          <Popconfirm
            title="Duyệt đội thi này?"
            description="Đội sẽ được chuyển sang trạng thái ACTIVE."
            onConfirm={onApprove}
            okText="Duyệt"
            cancelText="Hủy"
          >
            <Button
              type="primary"
              icon={<CheckOutlined />}
              loading={loading}
              style={approveStyle}
            >
              Duyệt
            </Button>
          </Popconfirm>
        </Tooltip>
      )}

      {canReject && (
        <Button
          danger
          icon={<CloseOutlined />}
          loading={loading}
          onClick={onReject}
          style={{ borderRadius: 8 }}
        >
          Từ chối
        </Button>
      )}

      {canDisband && disbandButton}
    </Space>
  );
};

export default TeamActionButtons;
