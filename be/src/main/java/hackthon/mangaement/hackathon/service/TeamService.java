package hackthon.mangaement.hackathon.service;

import hackthon.mangaement.hackathon.exception.BusinessRuleException;
import hackthon.mangaement.hackathon.exception.ConflictException;
import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.model.Team.Team;
import hackthon.mangaement.hackathon.model.Team.TeamMember;
import hackthon.mangaement.hackathon.model.Team.TeamRoundTrack;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.model.organization.Hackathon;
import hackthon.mangaement.hackathon.model.organization.Round;
import hackthon.mangaement.hackathon.model.organization.Track;
import hackthon.mangaement.hackathon.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private TeamRoundTrackRepository teamRoundTrackRepository;

    @Autowired
    private HackathonRepository hackathonRepository;

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogService auditLogService;

    public Team createTeam(String teamName, User leader, Integer hackathonId) {
        if (teamRepository.findByTeamName(teamName).isPresent()) {
            throw new ConflictException("Team name already exists: " + teamName);
        }

        // Verify leader does not belong to another ACTIVE team
        if (teamMemberRepository.existsByUserIdAndStatus(leader.getId(), TeamMember.Status.ACCEPTED)) {
            throw new ConflictException("User is already a member of an active team.");
        }

        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon not found."));

        Team team = Team.builder()
                .teamName(teamName)
                .leader(leader)
                .hackathon(hackathon)
                .status(Team.Status.PENDING)
                .isLocked(false)
                .build();
        Team savedTeam = teamRepository.save(team);

        // Leader joins automatically
        TeamMember member = TeamMember.builder()
                .team(savedTeam)
                .user(leader)
                .roleInTeam(TeamMember.RoleInTeam.LEADER)
                .status(TeamMember.Status.ACCEPTED)
                .joinedAt(LocalDateTime.now())
                .build();
        teamMemberRepository.save(member);

        return savedTeam;
    }

    public void inviteMember(Integer teamId, String memberEmail, User leader) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found."));

        if (team.getIsLocked()) {
            throw new BusinessRuleException("Cannot invite members: team is locked after deadline.");
        }

        if (!team.getLeader().getId().equals(leader.getId())) {
            throw new BusinessRuleException("Only the Team Leader can invite members.");
        }

        // Count current accepted + pending invitations
        List<TeamMember> currentMembers = teamMemberRepository.findByTeamId(teamId);
        long count = currentMembers.stream()
                .filter(m -> m.getStatus() == TeamMember.Status.ACCEPTED || m.getStatus() == TeamMember.Status.PENDING)
                .count();

        if (count >= 5) {
            throw new BusinessRuleException("Team is already full (maximum 5 members).");
        }

        User invitedUser = userRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + memberEmail));

        if (invitedUser.getStatus() != User.Status.APPROVED) {
            throw new BusinessRuleException("Invited user is not approved by Coordinator yet.");
        }

        // Check if already in another ACTIVE team
        if (teamMemberRepository.existsByUserIdAndStatus(invitedUser.getId(), TeamMember.Status.ACCEPTED)) {
            throw new ConflictException("Invited user is already in an active team.");
        }

        // Check if invitation already exists
        Optional<TeamMember> existingInvite = teamMemberRepository.findByTeamIdAndUserId(teamId, invitedUser.getId());
        if (existingInvite.isPresent()) {
            if (existingInvite.get().getStatus() == TeamMember.Status.PENDING) {
                throw new ConflictException("Invitation already pending for this user.");
            } else if (existingInvite.get().getStatus() == TeamMember.Status.ACCEPTED) {
                throw new ConflictException("User is already a member of this team.");
            } else {
                // If rejected or left, reactivate invite
                existingInvite.get().setStatus(TeamMember.Status.PENDING);
                existingInvite.get().setLeftAt(null);
                teamMemberRepository.save(existingInvite.get());
            }
        } else {
            TeamMember newMember = TeamMember.builder()
                    .team(team)
                    .user(invitedUser)
                    .roleInTeam(TeamMember.RoleInTeam.MEMBER)
                    .status(TeamMember.Status.PENDING)
                    .build();
            teamMemberRepository.save(newMember);
        }

        notificationService.sendNotification(invitedUser, "TEAM_INVITATION", "Team Invitation",
                "You have been invited to join team '" + team.getTeamName() + "' by " + leader.getFullName() + ".",
                "teams", teamId);
    }

    public void respondToInvitation(Integer teamId, User member, boolean accept) {
        TeamMember teamMember = teamMemberRepository.findByTeamIdAndUserId(teamId, member.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No invitation found for this team."));

        if (teamMember.getStatus() != TeamMember.Status.PENDING) {
            throw new BusinessRuleException("Invitation is already processed.");
        }

        Team team = teamMember.getTeam();
        if (team.getIsLocked()) {
            throw new BusinessRuleException("Cannot join: team is locked after deadline.");
        }

        if (accept) {
            // Re-verify they are not in another active team
            if (teamMemberRepository.existsByUserIdAndStatus(member.getId(), TeamMember.Status.ACCEPTED)) {
                throw new ConflictException("You are already a member of another active team.");
            }
            teamMember.setStatus(TeamMember.Status.ACCEPTED);
            teamMember.setJoinedAt(LocalDateTime.now());
            teamMemberRepository.save(teamMember);

            notificationService.sendNotification(team.getLeader(), "INVITATION_ACCEPTED", "Invitation Accepted",
                    member.getFullName() + " accepted your invitation to join team '" + team.getTeamName() + "'.",
                    "teams", teamId);
        } else {
            teamMember.setStatus(TeamMember.Status.REJECTED);
            teamMemberRepository.save(teamMember);

            notificationService.sendNotification(team.getLeader(), "INVITATION_REJECTED", "Invitation Rejected",
                    member.getFullName() + " declined your invitation to join team '" + team.getTeamName() + "'.",
                    "teams", teamId);
        }
    }

    public void approveTeam(Integer teamId, boolean approve, String reason, User coordinator, String ipAddress) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found."));

        if (team.getStatus() != Team.Status.PENDING) {
            throw new BusinessRuleException("Only PENDING teams can be approved.");
        }

        // Must have at least 3 members (status = ACCEPTED)
        List<TeamMember> acceptedMembers = teamMemberRepository.findByTeamIdAndStatus(teamId,
                TeamMember.Status.ACCEPTED);
        if (acceptedMembers.size() < 3) {
            throw new BusinessRuleException("Team cannot be approved: must have at least 3 accepted members (currently "
                    + acceptedMembers.size() + ").");
        }

        Team.Status oldStatus = team.getStatus();
        Team.Status newStatus = approve ? Team.Status.ACTIVE : Team.Status.REJECTED;
        team.setStatus(newStatus);
        team.setRejectionReason(approve ? null : reason);
        teamRepository.save(team);

        // Audit Logging
        Map<String, Object> detail = new HashMap<>();
        detail.put("teamId", teamId);
        detail.put("oldStatus", oldStatus.name());
        detail.put("newStatus", newStatus.name());
        detail.put("reason", reason);
        auditLogService.logAction(coordinator, approve ? "TEAM_APPROVE" : "TEAM_REJECT", "teams", teamId, detail,
                ipAddress);

        // Notifications
        String title = approve ? "Team Approved" : "Team Rejected";
        String body = approve ? "Congratulations! Team '" + team.getTeamName() + "' is now ACTIVE."
                : "Sorry, team '" + team.getTeamName() + "' was rejected. Reason: " + reason;

        for (TeamMember m : acceptedMembers) {
            notificationService.sendNotification(m.getUser(), approve ? "TEAM_APPROVED" : "TEAM_REJECTED", title, body,
                    "teams", teamId);
        }
    }

    public void lockTeams(Integer hackathonId, User coordinator, String ipAddress) {
        List<Team> teams = teamRepository.findByHackathonId(hackathonId);
        for (Team t : teams) {
            if (t.getStatus() == Team.Status.ACTIVE && !t.getIsLocked()) {
                t.setIsLocked(true);
                t.setLockedAt(LocalDateTime.now());
                teamRepository.save(t);

                // Audit Log
                Map<String, Object> detail = new HashMap<>();
                detail.put("teamId", t.getId());
                detail.put("lockedAt", t.getLockedAt());
                auditLogService.logAction(coordinator, "TEAM_LOCK", "teams", t.getId(), detail, ipAddress);
            }
        }
    }

    public void runLottery(Integer hackathonId, User coordinator, String ipAddress) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon not found."));

        List<Round> rounds = roundRepository.findByHackathonIdOrderBySequenceOrderAsc(hackathonId);
        Round prelimRound = rounds.stream()
                .filter(r -> r.getRoundType() == Round.RoundType.PRELIMINARY)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("No preliminary round found to run lottery."));

        List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelimRound.getId());
        if (tracks.isEmpty()) {
            throw new BusinessRuleException("No tracks found under preliminary round.");
        }

        List<Team> activeTeams = teamRepository.findByHackathonId(hackathonId).stream()
                .filter(t -> t.getStatus() == Team.Status.ACTIVE)
                .toList();

        if (activeTeams.isEmpty()) {
            throw new BusinessRuleException("No ACTIVE teams found in this hackathon.");
        }

        // Logic check: Spring 2026 vs Fall 2025
        boolean isSpring = hackathon.getName().contains("Spring") || hackathon.getSeason() == Hackathon.Season.Spring;

        if (isSpring) {
            // Spring 2026: BTC bốc thăm ngẫu nhiên. Track = 1 bảng, tối đa 8 đội.
            // 1. Shuffle teams
            List<Team> shuffledTeams = new ArrayList<>(activeTeams);
            Collections.shuffle(shuffledTeams);

            int trackIndex = 0;
            Map<Integer, Integer> trackTeamCounts = new HashMap<>();

            for (Team team : shuffledTeams) {
                // Find a track that has slot (count < max_teams_per_group / max_teams)
                int attemptCount = 0;
                Track assignedTrack = null;

                while (attemptCount < tracks.size()) {
                    Track track = tracks.get((trackIndex + attemptCount) % tracks.size());
                    int maxAllowed = track.getMaxTeamsPerGroup() != null ? track.getMaxTeamsPerGroup() : 8;
                    int currentCount = trackTeamCounts.getOrDefault(track.getId(), 0);

                    if (currentCount < maxAllowed) {
                        assignedTrack = track;
                        trackTeamCounts.put(track.getId(), currentCount + 1);
                        trackIndex = (trackIndex + attemptCount + 1) % tracks.size();
                        break;
                    }
                    attemptCount++;
                }

                if (assignedTrack == null) {
                    throw new BusinessRuleException(
                            "Lottery failure: Not enough slots in tracks to accommodate all active teams.");
                }

                // Check if already assigned
                Optional<TeamRoundTrack> trtOpt = teamRoundTrackRepository.findByTeamIdAndTrackId(team.getId(),
                        assignedTrack.getId());
                if (trtOpt.isEmpty()) {
                    TeamRoundTrack trt = TeamRoundTrack.builder()
                            .team(team)
                            .track(assignedTrack)
                            .assignedGroup(null) // Spring 2026: Single group per track (NULL)
                            .registrationType(TeamRoundTrack.RegistrationType.ASSIGNED)
                            .assignedBy(coordinator)
                            .assignedAt(LocalDateTime.now())
                            .build();
                    teamRoundTrackRepository.save(trt);
                }
            }
        } else {
            // Fall 2025: Đội lần lượt TỰ CHỌN Track (preferred). BTC bốc thăm chia bảng
            // ngẫu nhiên trong từng Track (mỗi bảng <= 6 đội).
            // Mock: Assign teams to tracks based on preferred track (or round-robin if not
            // set). Then group them.
            // For this logic, we will group the active teams already assigned to tracks
            // into brackets: A, B, C... of max 6 teams.
            // For any team not yet assigned, we assign round-robin.
            List<Team> shuffledTeams = new ArrayList<>(activeTeams);
            Collections.shuffle(shuffledTeams);

            // First: ensure every team has a track assignment. If not, assign round-robin.
            Map<Integer, List<Team>> trackTeamsMap = new HashMap<>();
            int roundRobinTrackIndex = 0;

            for (Team team : shuffledTeams) {
                List<TeamRoundTrack> existingTrts = teamRoundTrackRepository.findByTeamId(team.getId());
                Track assignedTrack = null;

                if (!existingTrts.isEmpty()) {
                    assignedTrack = existingTrts.get(0).getTrack();
                } else {
                    // Assign round-robin
                    assignedTrack = tracks.get(roundRobinTrackIndex % tracks.size());
                    roundRobinTrackIndex++;

                    TeamRoundTrack trt = TeamRoundTrack.builder()
                            .team(team)
                            .track(assignedTrack)
                            .registrationType(TeamRoundTrack.RegistrationType.PREFERRED)
                            .assignedBy(coordinator)
                            .assignedAt(LocalDateTime.now())
                            .build();
                    teamRoundTrackRepository.save(trt);
                }

                trackTeamsMap.computeIfAbsent(assignedTrack.getId(), k -> new ArrayList<>()).add(team);
            }

            // Now, within each Track, group teams into brackets of max 6 teams: Bảng A,
            // Bảng B, etc.
            for (Track track : tracks) {
                List<Team> teamsInTrack = trackTeamsMap.getOrDefault(track.getId(), new ArrayList<>());
                int groupSize = track.getMaxTeamsPerGroup() != null ? track.getMaxTeamsPerGroup() : 6;

                char groupChar = 'A';
                for (int i = 0; i < teamsInTrack.size(); i += groupSize) {
                    String groupName = "Bảng " + groupChar;
                    List<Team> groupTeams = teamsInTrack.subList(i, Math.min(i + groupSize, teamsInTrack.size()));

                    for (Team t : groupTeams) {
                        TeamRoundTrack trt = teamRoundTrackRepository.findByTeamIdAndTrackId(t.getId(), track.getId())
                                .orElseThrow(() -> new ResourceNotFoundException("Team assignment not found"));
                        trt.setAssignedGroup(groupName);
                        teamRoundTrackRepository.save(trt);
                    }
                    groupChar++;
                }
            }
        }

        // Log audit action
        Map<String, Object> detail = new HashMap<>();
        detail.put("hackathonId", hackathonId);
        detail.put("mode", isSpring ? "Spring 2026 (ASSIGNED)" : "Fall 2025 (PREFERRED + GROUPING)");
        auditLogService.logAction(coordinator, "TEAM_LOTTERY_EXECUTED", "hackathons", hackathonId, detail, ipAddress);
    }

    // Lấy thí sinh mồ côi
    public List<User> getOrphanUsers(Integer hackathonId) {
        return userRepository.findOrphansByHackathon(hackathonId);
    }

    // Lấy đội thiếu người
    public List<Team> getIncompleteTeams(Integer hackathonId) {
        return teamRepository.findIncompleteTeams(hackathonId);
    }

    // Tự động ghép đội (Matchmaking)
    @Transactional
    public void runMatchmaking(Integer hackathonId, User coordinator) {
        List<User> orphans = getOrphanUsers(hackathonId);
        List<Team> incompleteTeams = getIncompleteTeams(hackathonId);

        for (User orphan : orphans) {
            for (Team team : incompleteTeams) {
                if (teamMemberRepository.findByTeamId(team.getId()).size() < 4) { // Max size = 4
                    TeamMember newMember = new TeamMember();
                    newMember.setTeam(team);
                    newMember.setUser(orphan);
                    newMember.setStatus(TeamMember.Status.ACCEPTED);
                    teamMemberRepository.save(newMember);
                    break;
                }
            }
        }
    }

    // Admin tạo đội
    @Transactional
    public Team adminCreateTeam(String name, Integer leaderId, Integer hackathonId) {
        User leader = userRepository.findById(leaderId).orElseThrow();
        return createTeam(name, leader, hackathonId); // Tái sử dụng hàm cũ
    }

    // Admin thêm thành viên
    @Transactional
    public void adminAddMember(Integer teamId, Integer userId) {
        Team team = teamRepository.findById(teamId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();

        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(user);
        member.setStatus(TeamMember.Status.ACCEPTED);
        teamMemberRepository.save(member);
    }

    // Admin gộp đội
    @Transactional
    public void mergeTeams(Integer sourceTeamId, Integer targetTeamId) {
        Team source = teamRepository.findById(sourceTeamId).orElseThrow();
        Team target = teamRepository.findById(targetTeamId).orElseThrow();

        // Chuyển toàn bộ thành viên từ source sang target
        List<TeamMember> members = teamMemberRepository.findByTeamId(source.getId());
        for (TeamMember m : members) {
            m.setTeam(target);
            teamMemberRepository.save(m);
        }

        // Xóa đội source
        teamRepository.delete(source);
    }

    @Transactional
    public void confirmTeamFormation(Integer teamId, Integer userId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        if (!team.getLeader().getId().equals(userId)) {
            throw new BusinessRuleException("Only the Team Leader can confirm formation.");
        }
        team.setStatus(Team.Status.PENDING); // Assuming confirm puts it in PENDING state for approval
        teamRepository.save(team);
    }

    @Transactional
    public void updateTeamStatus(Integer teamId, String status, User user) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        team.setStatus(Team.Status.valueOf(status));
        teamRepository.save(team);
    }

    @Transactional
    public void bulkApproveTeams(List<Integer> teamIds, User coordinator) {
        for (Integer id : teamIds) {
            approveTeam(id, true, null, coordinator, "0.0.0.0");
        }
    }

    public Map<String, Object> getJourney(Integer teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with ID: " + teamId));

        Hackathon h = team.getHackathon();
        List<Round> rounds = roundRepository.findByHackathonIdOrderBySequenceOrderAsc(h.getId());
        List<TeamRoundTrack> trts = teamRoundTrackRepository.findByTeamId(teamId);

        List<Map<String, Object>> steps = new ArrayList<>();
        
        for (int i = 0; i < rounds.size(); i++) {
            Round round = rounds.get(i);
            
            TeamRoundTrack trt = trts.stream()
                    .filter(t -> t.getTrack() != null && t.getTrack().getRound().getId().equals(round.getId()))
                    .findFirst()
                    .orElse(null);

            Map<String, Object> step = new HashMap<>();
            step.put("roundId", round.getId());
            step.put("roundName", round.getName());
            step.put("trackId", trt != null ? trt.getTrack().getId() : null);
            step.put("trackName", trt != null ? trt.getTrack().getName() : "Chưa phân bảng");

            String status = "PENDING";
            
            if (trt != null) {
                if (round.getIsActive()) {
                    status = "ACTIVE";
                } else if (round.getScoringLocked()) {
                    if (i + 1 < rounds.size()) {
                        Round nextRound = rounds.get(i + 1);
                        boolean assignedToNext = trts.stream()
                                .anyMatch(t -> t.getTrack() != null && t.getTrack().getRound().getId().equals(nextRound.getId()));
                        status = assignedToNext ? "ADVANCED" : "ELIMINATED";
                    } else {
                        status = "ADVANCED";
                    }
                } else {
                    status = "ACTIVE";
                }
            } else {
                if (i > 0) {
                    boolean wasInPrior = false;
                    boolean priorLocked = false;
                    for (int j = 0; j < i; j++) {
                        Round priorRound = rounds.get(j);
                        boolean inPrior = trts.stream().anyMatch(t -> t.getTrack() != null && t.getTrack().getRound().getId().equals(priorRound.getId()));
                        if (inPrior) {
                            wasInPrior = true;
                            if (priorRound.getScoringLocked()) {
                                priorLocked = true;
                            }
                        }
                    }
                    if (wasInPrior && priorLocked) {
                        status = "ELIMINATED";
                    } else {
                        status = "PENDING";
                    }
                } else {
                    status = "PENDING";
                }
            }

            step.put("participationStatus", status);
            steps.add(step);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("teamId", teamId);
        result.put("teamName", team.getTeamName());
        result.put("steps", steps);
        return result;
    }
}
