// src/features/people/hooks/usePeopleManagement.js
import { useState, useEffect, useCallback } from 'react';
import { message } from 'antd';
import { peopleService } from '../services/peopleService';
import { teamService } from '../../teams/services/teamService';
import { trackService } from '../../tracks/services/trackService';
import { roundService } from '../../rounds/services/roundService';
import { getTeamErrorMessage } from '../../../shared/constants/teamErrors';

export const usePeopleManagement = (hackathonId) => {
  const [mentors, setMentors] = useState([]);
  const [judges, setJudges] = useState([]);
  const [tempJudges, setTempJudges] = useState([]);
  
  const [tracks, setTracks] = useState([]);
  const [rounds, setRounds] = useState([]);
  const [activeTeams, setActiveTeams] = useState([]);
  
  const [teamMentors, setTeamMentors] = useState([]);
  const [judgeAssignments, setJudgeAssignments] = useState([]);
  const [isLoading, setIsLoading] = useState(false);

  const fetchBaseData = useCallback(async () => {
    setIsLoading(true);
    try {
      // 1. Tải cấu trúc giải đấu và các Đội thi (Active)
      const [rRes, tRes, teamsRes] = await Promise.all([
        roundService.listByHackathon(hackathonId),
        trackService.listByHackathon(hackathonId),
        teamService.listByHackathon(hackathonId, { status: 'ACTIVE', size: 300 })
      ]);
      const roundList = Array.isArray(rRes) ? rRes : rRes?.items || [];
      const trackList = Array.isArray(tRes) ? tRes : tRes?.items || [];
      const teamList = Array.isArray(teamsRes) ? teamsRes : teamsRes?.items || [];

      setRounds(roundList);
      setTracks(trackList);
      setActiveTeams(teamList);

      // 2. Tải danh sách Users
      const [mRes, jRes, tempRes] = await Promise.all([
        peopleService.getUsersByRole('MENTOR').catch(() => []),
        peopleService.getUsersByRole('JUDGE').catch(() => []),
        peopleService.getTempJudges().catch(() => [])
      ]);
      setMentors(Array.isArray(mRes) ? mRes : mRes?.items || []);
      setJudges(Array.isArray(jRes) ? jRes : jRes?.items || []);
      setTempJudges(Array.isArray(tempRes) ? tempRes : tempRes?.items || []);

      // 3. Tải Lịch sử Phân công MENTOR (Cho từng Đội thi)
      const mentorPromises = teamList.map(team => 
        peopleService.getTeamMentors(team.id)
          .then(res => ({ team, data: Array.isArray(res) ? res : res?.items || [] }))
          .catch(() => ({ team, data: [] }))
      );
      const mentorResults = await Promise.all(mentorPromises);
      let allMentors = [];
      mentorResults.forEach(({ team, data }) => {
        data.forEach(m => {
          allMentors.push({
            id: `${team.id}_${m.roundId || m.round_id}`,
            team_id: team.id,
            team_name: team.teamName || team.team_name,
            round_id: m.roundId || m.round_id,
            mentor_id: m.mentorId || m.mentor_id || m.id,
            mentor_name: m.mentorName || m.name || m.fullName || m.full_name
          });
        });
      });
      setTeamMentors(allMentors);

      // 4. Tải Lịch sử Phân công GIÁM KHẢO (Theo Track & Vòng Chung kết)
      const trackJudgePromises = trackList.map(track => 
        peopleService.getTrackJudges(track.id)
          .then(res => ({ targetName: `Bảng: ${track.name}`, judges: Array.isArray(res) ? res : res?.items || [] }))
          .catch(() => ({ targetName: `Bảng: ${track.name}`, judges: [] }))
      );
      const finalRounds = roundList.filter(r => r.is_final || r.isFinal);
      const roundJudgePromises = finalRounds.map(round => 
        peopleService.getRoundJudges(round.id)
          .then(res => ({ targetName: `Vòng: ${round.name}`, judges: Array.isArray(res) ? res : res?.items || [] }))
          .catch(() => ({ targetName: `Vòng: ${round.name}`, judges: [] }))
      );
      
      const [trackJResults, roundJResults] = await Promise.all([
        Promise.all(trackJudgePromises), 
        Promise.all(roundJudgePromises)
      ]);
      
      let allJudges = [];
      [...trackJResults, ...roundJResults].forEach(({ targetName, judges }) => {
        judges.forEach(j => {
          allJudges.push({
            id: j.id || j.assignmentId,
            target_name: targetName,
            // Hỗ trợ đọc object lồng nhau từ BE (vd: j.judge.fullName)
            judge_name: j.judge?.fullName || j.judge?.name || j.user?.fullName || j.judgeName || j.fullName || j.name,
            // Lấy thêm person_id để UI quét chéo danh sách
            person_id: j.judge?.id || j.user?.id || j.judgeId || j.userId,
            assignment_type: j.assignmentType || j.assignment_type || 'NORMAL'
          });
        });
      });
      setJudgeAssignments(allJudges);

    } catch (err) {
      message.error('Lỗi khi đồng bộ dữ liệu nhân sự.');
    } finally {
      setIsLoading(false);
    }
  }, [hackathonId]);

  useEffect(() => {
    fetchBaseData();
  }, [fetchBaseData]);

  // Hành động gọi API POST / DELETE
  const createTempJudge = async (values, onSuccess) => {
    setIsLoading(true);
    try {
      await peopleService.createTempJudge({ ...values, hackathonId });
      message.success('Đã gửi lời mời Giám khảo khách mời thành công!');
      await fetchBaseData();
      if (onSuccess) onSuccess();
    } catch (error) {
      message.error(getTeamErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  };

  const assignMentor = async (values, onSuccess) => {
    setIsLoading(true);
    try {
      await teamService.assignMentor(values.team_id, values.round_id, values.mentor_id);
      message.success('Phân công Mentor thành công!');
      await fetchBaseData();
      if (onSuccess) onSuccess();
    } catch (error) {
      message.error(getTeamErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  };

  const removeMentor = async (teamId, roundId) => {
    setIsLoading(true);
    try {
      await teamService.removeMentor(teamId, roundId);
      message.success('Đã gỡ Mentor thành công!');
      await fetchBaseData();
    } catch (error) {
      message.error(getTeamErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  };

  const assignJudge = async (values, onSuccess) => {
    setIsLoading(true);
    try {
      let trackId = null;
      let roundId = null;
      const [targetType, targetId] = values.assignment_target.split('_');
      if (targetType === 'TRACK') trackId = parseInt(targetId);
      if (targetType === 'ROUND') roundId = parseInt(targetId);

      await peopleService.assignJudge({
        judgeId: values.person_id,
        trackId,
        roundId,
        assignmentType: values.assignment_type
      });
      message.success('Phân công Giám khảo thành công!');
      await fetchBaseData();
      if (onSuccess) onSuccess();
    } catch (error) {
      message.error(getTeamErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  };

  const removeJudge = async (assignmentId) => {
    setIsLoading(true);
    try {
      await peopleService.removeJudgeAssignment(assignmentId);
      message.success('Đã gỡ Giám khảo thành công!');
      await fetchBaseData();
    } catch (error) {
      message.error(getTeamErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  };

  return { 
    mentors, judges, tempJudges, tracks, rounds, activeTeams, teamMentors, judgeAssignments, isLoading, 
    createTempJudge, assignMentor, removeMentor, assignJudge, removeJudge 
  };
};