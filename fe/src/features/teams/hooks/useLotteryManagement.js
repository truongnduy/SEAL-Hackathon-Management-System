// src/features/teams/hooks/useLotteryManagement.js
import { useState, useEffect, useCallback } from 'react';
import { message } from 'antd';
import { teamService } from '../services/teamService';
import { trackService } from '../../tracks/services/trackService';
import { roundService } from '../../rounds/services/roundService';
import { getTeamErrorMessage } from '../../../shared/constants/teamErrors';
// THÊM: Import bộ chuyển đổi dữ liệu chuẩn của Track
import { mapTrackToBE } from '../../tracks/mappers/trackMapper'; 

export const useLotteryManagement = (hackathonId) => {
  const [rounds, setRounds] = useState([]);
  const [tracks, setTracks] = useState([]);
  const [activeTeams, setActiveTeams] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [selectedRoundId, setSelectedRoundId] = useState(null);

  // 1. Tải toàn bộ dữ liệu cần thiết
  const fetchLotteryData = useCallback(async () => {
    setIsLoading(true);
    try {
      const [rRes, tRes, teamsRes] = await Promise.all([
        roundService.listByHackathon(hackathonId),
        trackService.listByHackathon(hackathonId),
        teamService.listByHackathon(hackathonId, { status: 'ACTIVE' }) // Chỉ lấy đội đã được duyệt
      ]);

      const roundList = Array.isArray(rRes) ? rRes : rRes?.items || [];
      const trackList = Array.isArray(tRes) ? tRes : tRes?.items || [];
      const teamList = Array.isArray(teamsRes) ? teamsRes : teamsRes?.items || [];

      setRounds(roundList.filter(r => !r.is_final && !r.isFinal)); // Chỉ lấy vòng Sơ loại để bốc thăm
      setTracks(trackList);
      setActiveTeams(teamList);

      if (!selectedRoundId && roundList.length > 0) {
        const firstPrelim = roundList.find(r => !r.is_final && !r.isFinal);
        if (firstPrelim) setSelectedRoundId(firstPrelim.id);
      }
    } catch (error) {
      message.error('Lỗi khi tải dữ liệu Bốc thăm & Bảng đấu');
    } finally {
      setIsLoading(false);
    }
  }, [hackathonId, selectedRoundId]);

  useEffect(() => {
    fetchLotteryData();
  }, [fetchLotteryData]);

// 2. Logic Cập nhật Topic cho Bảng đấu
  const handleAssignTopic = async (trackId, newTopic, currentTrackData) => {
    setIsLoading(true);
    try {
      // Chuyển đổi dữ liệu sang snake_case chuẩn xác để Mapper không bị undefined
      const feData = {
        name: currentTrackData.name,
        description: currentTrackData.description,
        topic: newTopic, // FE vẫn gửi Topic mới bình thường
        max_teams: currentTrackData.maxTeams ?? currentTrackData.max_teams,
        max_teams_per_group: currentTrackData.maxTeamsPerGroup ?? currentTrackData.max_teams_per_group,
        min_team_size: currentTrackData.minTeamSize ?? currentTrackData.min_team_size ?? 1,
        max_team_size: currentTrackData.maxTeamSize ?? currentTrackData.max_team_size ?? 5,
        status: currentTrackData.status || 'OPEN'
      };

      // Đưa qua Mapper của dự án
      const payload = mapTrackToBE(feData);

      // Gọi API PUT
      await trackService.update(trackId, payload);
      message.success('Đã gửi yêu cầu cập nhật lên hệ thống!');
      await fetchLotteryData();
    } catch (error) {
      message.error(getTeamErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  };

  // 3. Logic Bốc thăm tự động (Bulk Lottery)
  const handleRunAutoLottery = async () => {
    if (!selectedRoundId) return message.warning('Vui lòng chọn Vòng thi trước!');
    setIsLoading(true);
    try {
      // FE chỉ cần gửi đúng roundId, BE sẽ lo thuật toán chia bảng
      await teamService.runLottery(hackathonId, { roundId: selectedRoundId });
      message.success('Bốc thăm phân bảng thành công cho tất cả các đội!');
      await fetchLotteryData();
    } catch (error) {
      message.error(getTeamErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  };

  // 4. Logic Đổi Bảng đấu cho 1 đội cụ thể (Re-Lottery)
  const handleChangeTrack = async (teamId, newTrackId) => {
    if (!selectedRoundId) return;
    setIsLoading(true);
    try {
      // FIX LỖI 400: Ép kiểu Number và gửi bao vây cả 2 định dạng cho Track
      const payload = {
        trackId: Number(newTrackId),
        track_id: Number(newTrackId)
      };

      // Gọi API đổi track
      await teamService.changeTrack(teamId, selectedRoundId, payload.trackId); // URL lấy theo trackId
      // Sửa lại file teamService.js ở Bước 1 một chút nếu BE yêu cầu đổi body.
      // Nhưng hiện tại ta cứ test API Bốc thăm tự động trước nhé!
      message.success('Đã đổi Bảng đấu cho đội thi thành công!');
      await fetchLotteryData();
    } catch (error) {
      message.error(getTeamErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  };

  return {
    rounds,
    tracks,
    activeTeams,
    isLoading,
    selectedRoundId,
    setSelectedRoundId,
    handleAssignTopic,
    handleRunAutoLottery,
    handleChangeTrack,
    refetch: fetchLotteryData
  };
};