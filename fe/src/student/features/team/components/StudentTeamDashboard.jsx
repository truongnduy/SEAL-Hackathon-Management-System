/**
 * Component: StudentTeamDashboard
 * Chức năng: Layout chính của màn hình quản lý đội (khi sinh viên đã có đội). Bao gồm danh sách thành viên và thông tin tổng quan.
 */
import { Row, Col, Space } from 'antd';
import { motion } from 'framer-motion';
import TeamMemberManager from './TeamMemberManager';
import TeamOverviewCard from './TeamOverviewCard';
import FinalSubmissionPanel from '../../submission/components/FinalSubmissionPanel';
import RoundProblemPanel from '../../round/components/RoundProblemPanel';
import FinalRoundProblemPanel from '../../round/components/FinalRoundProblemPanel';

const StudentTeamDashboard = ({ 
  selectedTeam, 
  hackathonId,
  isActionLoading, 
  inviteMember, 
  cancelPendingInvite, 
  leaveTeam, 
  kickMember,
  transferLeader, 
  disbandTeam, 
  confirmTeamFormation,
  fetchInvitations 
}) => {
  return (
    <motion.div 
      key="dashboard"
      initial={{ opacity: 0, scale: 0.98 }}
      animate={{ opacity: 1, scale: 1 }}
      exit={{ opacity: 0, scale: 0.98 }}
      transition={{ duration: 0.4 }}
    >
      <Row gutter={[32, 32]}>
        <Col xs={24} lg={15} xl={16}>
          <Space direction="vertical" size={24} style={{ width: '100%' }}>
            <TeamMemberManager
              team={selectedTeam}
              loading={isActionLoading}
              onInviteMember={inviteMember}
              onCancelInvite={cancelPendingInvite}
              onLeaveTeam={async (teamId) => {
                const success = await leaveTeam(teamId);
                if (success) fetchInvitations();
              }}
              onKickMember={kickMember}
              onTransferLeader={transferLeader}
              onDisbandTeam={async (teamId) => {
                const success = await disbandTeam(teamId);
                if (success) fetchInvitations();
              }}
            />
            {selectedTeam?.id && (hackathonId || selectedTeam?.hackathonId) && (
              <RoundProblemPanel
                team={selectedTeam}
                hackathonId={hackathonId || selectedTeam.hackathonId}
              />
            )}
            {selectedTeam?.id && (hackathonId || selectedTeam?.hackathonId) && (
              <FinalRoundProblemPanel
                teamId={selectedTeam.id}
                hackathonId={hackathonId || selectedTeam.hackathonId}
              />
            )}
            {selectedTeam?.id && (hackathonId || selectedTeam?.hackathonId) && (
              <FinalSubmissionPanel
                teamId={selectedTeam.id}
                hackathonId={hackathonId || selectedTeam.hackathonId}
              />
            )}
          </Space>
        </Col>

        <Col xs={24} lg={9} xl={8}>
          <div style={{ position: 'sticky', top: 24 }}>
            <TeamOverviewCard
              team={selectedTeam}
              onConfirmFormation={confirmTeamFormation}
              actionLoading={isActionLoading}
            />
          </div>
        </Col>
      </Row>
    </motion.div>
  );
};

export default StudentTeamDashboard;

