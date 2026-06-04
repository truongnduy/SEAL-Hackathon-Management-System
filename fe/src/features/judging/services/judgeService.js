/**
 * TẠM THỜI SỬ DỤNG MOCK DATA VÌ BACKEND CHƯA CÓ API.
 * Sau này Backend làm xong, chỉ cần đổi nội dung trong các hàm này thành axiosClient.get(...)
 */
export const judgeService = {
  // 1. Lấy thông kê cho Dashboard
  getDashboardStats: async () => {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          stats: { totalEvaluated: 15, pendingEvaluations: 8, calibrationScore: 92 },
          assignments: [
            {
              id: 101,
              hackathonName: 'SEAL Hackathon Spring 2026',
              role: 'HEAD_JUDGE',
              trackName: 'AI & Machine Learning',
              roundName: 'Chung Kết',
              status: 'ONGOING',
              progress: 65,
              totalTeams: 20,
              scoredTeams: 13,
              date: '2026-06-05',
            },
            {
              id: 102,
              hackathonName: 'SEAL Hackathon Summer 2026',
              role: 'NORMAL_JUDGE',
              trackName: 'Web Development',
              roundName: 'Sơ Loại',
              status: 'UPCOMING',
              progress: 0,
              totalTeams: 45,
              scoredTeams: 0,
              date: '2026-08-15',
            }
          ],
          upcomingEvents: [
            { id: 1, title: 'Họp Hiệu chuẩn Giám khảo', time: '08:00 AM, 01/06/2026', type: 'CALIBRATION' },
            { id: 2, title: 'Bắt đầu chấm thi: Vòng Sơ loại', time: '09:00 AM, 02/06/2026', type: 'SCORING' }
          ]
        });
      }, 600); // Giả lập mạng chậm 0.6s
    });
  },

  // 2. Lấy danh sách đội thi trong 1 assignment
  getTeamsToScore: async (assignmentId) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve([
          { id: 1, name: 'Cyber Nurture', leader: 'Nguyễn Văn A', status: 'PENDING' },
          { id: 2, name: 'Tech Innovators', leader: 'Trần Thị B', status: 'SCORED', totalScore: 85.5 },
          { id: 3, name: 'SEAL Alpha', leader: 'Lê Văn C', status: 'PENDING' },
          { id: 4, name: 'FPT Go', leader: 'Phạm Văn D', status: 'PENDING' },
        ]);
      }, 500);
    });
  },

  // 3. Lấy danh sách tiêu chí chấm điểm (Chuẩn theo cấu trúc Database của nhóm)
  getScoringCriteria: async (assignmentId) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve([
          { id: 1, name: 'Tính Đổi mới & Sáng tạo', type: 'Innovation', weight: 0.30, max_score: 100, description: 'Đánh giá mức độ độc đáo của giải pháp so với thị trường.' },
          { id: 2, name: 'Chất lượng Kỹ thuật', type: 'Technical', weight: 0.40, max_score: 100, description: 'Đánh giá kiến trúc phần mềm, clean code và hiệu năng.' },
          { id: 3, name: 'Kỹ năng Thuyết trình', type: 'General', weight: 0.30, max_score: 100, description: 'Đánh giá kỹ năng trình bày, slide và trả lời câu hỏi (Q&A).' },
        ]);
      }, 500);
    });
  },

  // 4. Gửi điểm lên server
  submitScore: async (assignmentId, teamId, payload) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({ success: true, message: 'Saved successfully' });
      }, 800);
    });
  }
};