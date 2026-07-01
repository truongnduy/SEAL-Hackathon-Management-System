import axiosClient from '../../../../shared/api/axiosClient';
import { ENDPOINTS } from '../../../../shared/api/endpoints';
import { mapMatchmakingTeam } from '../../../../features/teams/mappers/matchmaking.mapper';

const unwrapList = (res) => {
  if (Array.isArray(res)) return res;
  if (Array.isArray(res?.data)) return res.data;
  return [];
};

export const matchmakingService = {
  getTeams: async (hackathonId) => {
    const res = await axiosClient.get(ENDPOINTS.TEAMS.MATCHMAKING(hackathonId));
    return unwrapList(res).map(mapMatchmakingTeam).filter(Boolean);
  },
};
