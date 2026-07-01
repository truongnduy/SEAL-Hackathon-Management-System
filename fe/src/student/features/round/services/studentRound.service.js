import axiosClient from '../../../../shared/api/axiosClient';

export const studentRoundService = {
  getCurrentDeadline: async (hackathonId) =>
    axiosClient.get('/api/v1/me/rounds/current/deadline', {
      params: hackathonId ? { hackathonId } : undefined,
    }),

  getProblem: async (roundId) => axiosClient.get(`/api/v1/me/rounds/${roundId}/problem`),

  downloadProblemStatement: async (roundId) =>
    axiosClient.get(`/api/v1/me/rounds/${roundId}/problem-statement`, {
      responseType: 'blob',
    }),

  getFinalRound: async (hackathonId) =>
    axiosClient.get(`/api/v1/me/hackathons/${hackathonId}/final-round`),
};
