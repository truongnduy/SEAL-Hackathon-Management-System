const DEFAULT_MAX_TEAM_MEMBERS = 5;

export const getLeaderEmail = (team) => {
  if (!team?.members?.length) return '';
  const leader = team.members.find((m) => m.roleInTeam === 'LEADER');
  return leader?.email || '';
};

export const mapMatchmakingTeam = (be) => {
  if (!be) return null;
  return {
    teamId: be.id ?? be.teamId,
    teamName: be.teamName,
    memberCount: be.acceptedMemberCount ?? be.currentMemberCount ?? 0,
    maxMembers: be.maxTeamSize ?? DEFAULT_MAX_TEAM_MEMBERS,
    minMembers: be.minTeamSize ?? 3,
    leaderId: be.leaderId,
    leaderName: be.leaderName,
    leaderEmail: be.leaderEmail || getLeaderEmail(be),
    status: be.status,
    isLocked: be.isLocked,
    members: be.members,
  };
};

export const mapOrphanUser = (be) => {
  if (!be) return null;
  return {
    id: be.id,
    fullName: be.fullName,
    email: be.email,
    status: be.status,
  };
};
