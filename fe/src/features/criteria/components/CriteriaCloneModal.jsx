import React, { useState, useEffect } from "react";
import {
  Modal,
  Select,
  Alert,
  Typography,
  Switch,
  Space,
  Spin,
  message,
  theme,
} from "antd";
import axiosClient from "../../../shared/api/axiosClient";
import { ENDPOINTS } from "../../../shared/api/endpoints";
import { criteriaService } from "../services/criteriaService";

const { Text } = Typography;
const { Option } = Select;
const { useToken } = theme;

export const CriteriaCloneModal = ({
  visible,
  onCancel,
  onClone,
  currentHackathonId,
  currentRound,
  selectedRoundId,
  selectedTrackId,
}) => {
  const { token } = useToken();
  const [hackathons, setHackathons] = useState([]);
  const [sourceRounds, setSourceRounds] = useState([]);
  const [trackCloneSources, setTrackCloneSources] = useState([]);
  const [selectedHackathonId, setSelectedHackathonId] = useState(null);
  const [cloneSourceId, setCloneSourceId] = useState(null);
  const [cloneSourceType, setCloneSourceType] = useState("TRACK");
  const [replaceExisting, setReplaceExisting] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  // === CÁC HÀM XỬ LÝ LOGIC (GIỮ NGUYÊN 100%) ===
  const extractArray = (res) => {
    if (!res) return [];
    if (Array.isArray(res)) return res;
    if (Array.isArray(res.items)) return res.items;
    if (Array.isArray(res.content)) return res.content;
    if (Array.isArray(res.data)) return res.data;
    if (res.data) {
      if (Array.isArray(res.data.items)) return res.data.items;
      if (Array.isArray(res.data.content)) return res.data.content;
    }
    return [];
  };

  useEffect(() => {
    if (visible) {
      setCloneSourceId(null);
      setReplaceExisting(false);
      fetchHackathons();
      if (currentRound?.is_final) {
        setCloneSourceType("ROUND");
        setSelectedHackathonId(null);
      } else {
        setCloneSourceType("TRACK");
        setSelectedHackathonId(currentHackathonId);
        if (selectedTrackId) fetchTrackCloneSources(selectedTrackId);
      }
    } else {
      setSelectedHackathonId(null);
      setCloneSourceId(null);
      setReplaceExisting(false);
    }
  }, [visible, currentRound, currentHackathonId, selectedTrackId]);

  useEffect(() => {
    if (selectedHackathonId && visible && currentRound?.is_final)
      fetchRoundsForFinal(selectedHackathonId);
  }, [selectedHackathonId, visible, currentRound]);

  const fetchHackathons = async () => {
    try {
      const res = await axiosClient.get(ENDPOINTS.HACKATHONS.BASE);
      setHackathons(extractArray(res));
    } catch (error) {
      message.error("Lỗi tải danh sách Hackathon");
    }
  };

  const fetchRoundsForFinal = async (hackId) => {
    setIsLoading(true);
    setCloneSourceId(null);
    try {
      const res = await axiosClient.get(ENDPOINTS.HACKATHONS.ROUNDS(hackId));
      setSourceRounds(extractArray(res));
    } catch (error) {
      message.error("Lỗi tải dữ liệu Vòng thi");
    } finally {
      setIsLoading(false);
    }
  };

  const fetchTrackCloneSources = async (trackId) => {
    setIsLoading(true);
    setCloneSourceId(null);
    try {
      const res = await criteriaService.getCloneSourcesForTrack(trackId);
      let sources = [];
      if (res && Array.isArray(res.sources)) sources = res.sources;
      else if (res?.data && Array.isArray(res.data.sources))
        sources = res.data.sources;
      else if (res?.data?.data && Array.isArray(res.data.data.sources))
        sources = res.data.data.sources;
      setTrackCloneSources(sources);
    } catch (error) {
      message.error("Lỗi tải danh sách bảng đấu nguồn");
    } finally {
      setIsLoading(false);
    }
  };

  const handleOk = () => {
    if (cloneSourceId) onClone(cloneSourceType, cloneSourceId, replaceExisting);
  };

  const filteredTrackSources = trackCloneSources.filter(
    (s) => s.hackathonId === selectedHackathonId,
  );

  // === RENDER GIAO DIỆN MỚI ===
  return (
    <Modal
      title={
        <span style={{ fontSize: 18, fontWeight: 600 }}>
          {currentRound?.is_final
            ? "Sao chép từ Mùa giải khác"
            : "Sao chép từ Bảng đấu khác"}
        </span>
      }
      open={visible}
      onOk={handleOk}
      onCancel={onCancel}
      okText="Tiến hành Sao chép"
      cancelText="Hủy"
      okButtonProps={{
        disabled: !cloneSourceId || isLoading,
        size: "large",
        style: { borderRadius: 8 },
      }}
      cancelButtonProps={{ size: "large", style: { borderRadius: 8 } }}
      width={640}
      styles={{ content: { borderRadius: 16 } }}
    >
      <div style={{ paddingTop: 8 }}>
        <div
          style={{
            marginBottom: 24,
            padding: "16px",
            backgroundColor: replaceExisting
              ? token.colorErrorBg
              : token.colorSuccessBg,
            borderRadius: 12,
            border: `1px solid ${replaceExisting ? token.colorErrorBorder : token.colorSuccessBorder}`,
            transition: "all 0.3s ease",
          }}
        >
          <Space
            size="middle"
            style={{
              marginBottom: 12,
              display: "flex",
              justifyContent: "space-between",
            }}
          >
            <Text strong style={{ fontSize: 15 }}>
              Chế độ xử lý dữ liệu hiện tại:
            </Text>
            <Switch
              checkedChildren="Ghi đè tất cả"
              unCheckedChildren="Cộng dồn"
              checked={replaceExisting}
              onChange={setReplaceExisting}
            />
          </Space>
          <div style={{ fontSize: "14px" }}>
            {replaceExisting ? (
              <span style={{ color: token.colorError }}>
                <strong style={{ display: "inline-block", marginRight: 6 }}>
                  CẢNH BÁO:
                </strong>
                Toàn bộ tiêu chí hiện tại của vòng/bảng này sẽ bị xóa sạch và
                thay thế hoàn toàn.
              </span>
            ) : (
              <span style={{ color: token.colorSuccess}}>
                <strong style={{ display: "inline-block", marginRight: 6}}>
                  AN TOÀN:
                </strong>
                Dữ liệu sao chép sẽ được nối tiếp vào danh sách tiêu chí hiện
                tại của bạn.
              </span>
            )}
          </div>
        </div>

        <Spin spinning={isLoading}>
          <div style={{ marginBottom: 20 }}>
            <Text strong style={{ display: "block", marginBottom: 8 }}>
              1. Chọn Mùa giải (Hackathon) nguồn:
            </Text>
            <Select
              size="large"
              style={{ width: "100%" }}
              placeholder="Tìm kiếm mùa giải..."
              value={selectedHackathonId}
              onChange={(val) => {
                setSelectedHackathonId(val);
                setCloneSourceId(null);
              }}
              showSearch
              optionFilterProp="children"
            >
              {hackathons.map((h) => {
                const disabledHackathon = currentRound?.is_final
                  ? h.id === currentHackathonId
                  : false;
                return (
                  <Option key={h.id} value={h.id} disabled={disabledHackathon}>
                    {h.name}
                  </Option>
                );
              })}
            </Select>
          </div>

          <div style={{ marginBottom: 24 }}>
            <Text strong style={{ display: "block", marginBottom: 8 }}>
              2. Chọn Vòng thi / Bảng đấu nguồn để chép:
            </Text>
            <Select
              size="large"
              style={{ width: "100%" }}
              placeholder="Vui lòng chọn nguồn dữ liệu..."
              value={
                cloneSourceId
                  ? `${cloneSourceType}_${cloneSourceId}`
                  : undefined
              }
              onChange={(val) => {
                const [type, id] = val.split("_");
                setCloneSourceType(type);
                setCloneSourceId(parseInt(id));
              }}
              disabled={
                currentRound?.is_final
                  ? !selectedHackathonId || sourceRounds.length === 0
                  : filteredTrackSources.length === 0
              }
            >
              {currentRound?.is_final
                ? sourceRounds.map((r) => {
                    const isRoundFinal =
                      r.is_final === true ||
                      r.isFinal === true ||
                      r.name?.toLowerCase().includes("chung kết") ||
                      r.name?.toLowerCase().includes("final");
                    if (!isRoundFinal) return null;
                    const isSameRound =
                      selectedHackathonId === currentHackathonId &&
                      r.id === selectedRoundId;
                    return (
                      <Option
                        key={`ROUND_${r.id}`}
                        value={`ROUND_${r.id}`}
                        disabled={isSameRound}
                      >
                        🏆 Vòng Chung kết: {r.name}
                      </Option>
                    );
                  })
                : filteredTrackSources.map((s) => (
                    <Option
                      key={`TRACK_${s.trackId}`}
                      value={`TRACK_${s.trackId}`}
                    >
                      Bảng: {s.trackName} ({s.criteriaCount} tiêu chí)
                    </Option>
                  ))}
            </Select>
          </div>
        </Spin>

        <Alert
          type="info"
          showIcon
          style={{ borderRadius: 8 }}
          message={
            currentRound?.is_final
              ? "Vòng Chung kết chỉ có thể sao chép bộ tiêu chí từ các vòng Chung kết khác."
              : "Bạn có thể chọn Mùa giải bất kỳ ở trên để tìm kiếm các bảng đấu có sẵn tiêu chí để sao chép."
          }
        />
      </div>
    </Modal>
  );
};
