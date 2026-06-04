// src/features/people/services/peopleService.js
import axiosClient from '../../../shared/api/axiosClient';

export const peopleService = {
  // Lấy danh sách Users theo vai trò
  getUsersByRole: async (role) => {
    return axiosClient.get('/api/users', { params: { role, status: 'APPROVED', size: 500 } });
  },

  // Quản lý Giám khảo Khách mời (FR-10)
  createTempJudge: async (data) => axiosClient.post('/api/users/temp-judges', data),
  getTempJudges: async () => axiosClient.get('/api/users/temp-judges', { params: { size: 500 } }),
  
  // Phân công Giám khảo (Judge Assignments)
  assignJudge: async (data) => axiosClient.post('/api/judge-assignments', data),
  removeJudgeAssignment: async (id) => axiosClient.delete(`/api/judge-assignments/${id}`),
  getTrackJudges: async (trackId) => axiosClient.get(`/api/tracks/${trackId}/judges`),
  getRoundJudges: async (roundId) => axiosClient.get(`/api/rounds/${roundId}/judges`),

  // Lịch sử Mentor theo Đội thi
  getTeamMentors: async (teamId) => axiosClient.get('/api/teams/' + teamId + '/mentors'),
};