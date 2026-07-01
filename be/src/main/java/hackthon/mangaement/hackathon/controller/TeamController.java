package hackthon.mangaement.hackathon.controller;

import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.model.Team.Team;
import hackthon.mangaement.hackathon.model.Team.TeamMember;
import hackthon.mangaement.hackathon.model.Team.TeamRoundTrack;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.model.organization.Track;
import hackthon.mangaement.hackathon.model.Mentor.MentorAssignment;
import hackthon.mangaement.hackathon.repository.*;
import hackthon.mangaement.hackathon.service.HackathonService;
import hackthon.mangaement.hackathon.service.TeamService;
import hackthon.mangaement.hackathon.dto.AdminCreateTeamRequest;
import hackthon.mangaement.hackathon.dto.AdminAddMemberRequest;
import hackthon.mangaement.hackathon.dto.AdminMergeTeamRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @Autowired
    private HackathonService hackathonService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private TeamRoundTrackRepository teamRoundTrackRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MentorAssignmentRepository mentorAssignmentRepository;

    @PostMapping("/teams")
    public ResponseEntity<?> createTeam(@RequestBody Map<String, Object> req,
            @AuthenticationPrincipal User leader) {
        Team team = teamService.createTeam(
                (String) req.get("teamName"),
                leader,
                (Integer) req.get("hackathonId"));
        return ResponseEntity.ok(team);
    }

    @PostMapping("/teams/{id}/members/invite")
    public ResponseEntity<?> inviteMember(@PathVariable Integer id,
            @RequestBody Map<String, String> req,
            @AuthenticationPrincipal User leader) {
        teamService.inviteMember(id, req.get("email"), leader);
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Invitation sent successfully.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/teams/{id}/respond")
    public ResponseEntity<?> respondInvitation(@PathVariable Integer id,
            @RequestBody Map<String, Boolean> req,
            @AuthenticationPrincipal User member) {
        teamService.respondToInvitation(id, member, req.get("accept"));
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Responded to team invitation successfully.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/teams/{id}/approve")
    public ResponseEntity<?> approveTeam(@PathVariable Integer id,
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal User coordinator,
            HttpServletRequest servletRequest) {
        boolean approve = (Boolean) req.get("approve");
        String reason = (String) req.get("reason");
        teamService.approveTeam(id, approve, reason, coordinator, servletRequest.getRemoteAddr());
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Team approval status updated.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/hackathons/{id}/teams/lock")
    public ResponseEntity<?> lockTeams(@PathVariable Integer id,
            @AuthenticationPrincipal User coordinator,
            HttpServletRequest servletRequest) {
        teamService.lockTeams(id, coordinator, servletRequest.getRemoteAddr());
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "All active teams in this hackathon have been locked.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/hackathons/{id}/lottery")
    public ResponseEntity<?> runLottery(@PathVariable Integer id,
            @AuthenticationPrincipal User coordinator,
            HttpServletRequest servletRequest) {
        teamService.runLottery(id, coordinator, servletRequest.getRemoteAddr());
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Lottery run completed. Teams allocated to tracks and brackets.");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/teams")
    public ResponseEntity<?> getTeams(@RequestParam(required = false) Integer hackathonId) {
        if (hackathonId != null) {
            return ResponseEntity.ok(teamRepository.findByHackathonId(hackathonId));
        }
        return ResponseEntity.ok(teamRepository.findAll());
    }

    @GetMapping("/teams/{id}")
    public ResponseEntity<?> getTeamById(@PathVariable Integer id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with ID: " + id));
        return ResponseEntity.ok(team);
    }

    @DeleteMapping("/teams/{id}")
    public ResponseEntity<?> deleteTeam(@PathVariable Integer id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with ID: " + id));
        teamRepository.delete(team);
        return ResponseEntity.ok(Map.of("message", "Team deleted successfully."));
    }

    @PatchMapping("/teams/{teamId}/members/{userId}")
    public ResponseEntity<?> updateMemberStatus(@PathVariable Integer teamId,
            @PathVariable Integer userId,
            @RequestBody Map<String, String> req) {
        String action = req.get("action");
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in team."));

        if ("ACCEPTED".equalsIgnoreCase(action)) {
            member.setStatus(TeamMember.Status.ACCEPTED);
            member.setJoinedAt(LocalDateTime.now());
        } else if ("REJECTED".equalsIgnoreCase(action)) {
            member.setStatus(TeamMember.Status.REJECTED);
        } else if ("LEFT".equalsIgnoreCase(action)) {
            teamMemberRepository.delete(member);
            return ResponseEntity.ok(Map.of("message", "Member left the team successfully."));
        } else {
            member.setStatus(TeamMember.Status.valueOf(action));
        }

        teamMemberRepository.save(member);
        return ResponseEntity.ok(member);
    }

    @DeleteMapping("/teams/{teamId}/members/{userId}")
    public ResponseEntity<?> kickMember(@PathVariable Integer teamId,
            @PathVariable Integer userId) {
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in team."));
        teamMemberRepository.delete(member);
        return ResponseEntity.ok(Map.of("message", "Member removed from team successfully."));
    }

    @PatchMapping("/teams/{teamId}/transfer-leader")
    public ResponseEntity<?> transferLeader(@PathVariable Integer teamId,
            @RequestBody Map<String, Object> req) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found."));

        Integer newLeaderId = ((Number) req.get("newLeaderId")).intValue();
        User newLeader = userRepository.findById(newLeaderId)
                .orElseThrow(() -> new ResourceNotFoundException("New leader not found."));

        team.setLeader(newLeader);

        Optional<TeamMember> currentLeaderMember = teamMemberRepository.findByTeamIdAndUserId(teamId,
                team.getLeader().getId());
        if (currentLeaderMember.isPresent()) {
            currentLeaderMember.get().setRoleInTeam(TeamMember.RoleInTeam.MEMBER);
            teamMemberRepository.save(currentLeaderMember.get());
        }

        Optional<TeamMember> newLeaderMember = teamMemberRepository.findByTeamIdAndUserId(teamId, newLeaderId);
        if (newLeaderMember.isPresent()) {
            newLeaderMember.get().setRoleInTeam(TeamMember.RoleInTeam.LEADER);
            teamMemberRepository.save(newLeaderMember.get());
        }

        teamRepository.save(team);
        return ResponseEntity.ok(team);
    }

    @PatchMapping("/teams/{teamId}/rounds/{roundId}/track")
    public ResponseEntity<?> updateTeamTrack(@PathVariable Integer teamId,
            @PathVariable Integer roundId,
            @RequestBody Map<String, Object> req) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found."));
        Integer trackId = ((Number) req.get("trackId")).intValue();
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track not found."));

        Optional<TeamRoundTrack> trtOpt = teamRoundTrackRepository.findByTeamIdAndTrackId(teamId, trackId);
        TeamRoundTrack trt;
        if (trtOpt.isPresent()) {
            trt = trtOpt.get();
        } else {
            trt = TeamRoundTrack.builder()
                    .team(team)
                    .track(track)
                    .registrationType(TeamRoundTrack.RegistrationType.ASSIGNED)
                    .assignedAt(LocalDateTime.now())
                    .build();
        }
        teamRoundTrackRepository.save(trt);
        return ResponseEntity.ok(trt);
    }

    @PostMapping("/teams/{teamId}/rounds/{roundId}/mentor")
    public ResponseEntity<?> assignMentorToTeamTrack(@PathVariable Integer teamId,
            @PathVariable Integer roundId,
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal User coordinator) {
        if (coordinator == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        Integer mentorId = ((Number) req.get("mentorId")).intValue();
        User mentor = userRepository.findById(mentorId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor not found."));

        Track track = teamRoundTrackRepository.findByTeamId(teamId).stream()
                .map(TeamRoundTrack::getTrack)
                .filter(t -> t.getRound().getId().equals(roundId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No track assignment found for team in this round."));

        MentorAssignment assignment = hackathonService.assignMentor(mentor, track, coordinator);
        return ResponseEntity.ok(assignment);
    }

    @DeleteMapping("/teams/{teamId}/rounds/{roundId}/mentor")
    public ResponseEntity<?> removeMentorFromTeamTrack(@PathVariable Integer teamId,
            @PathVariable Integer roundId) {
        Track track = teamRoundTrackRepository.findByTeamId(teamId).stream()
                .map(TeamRoundTrack::getTrack)
                .filter(t -> t.getRound().getId().equals(roundId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No track assignment found for team in this round."));

        List<MentorAssignment> assignments = mentorAssignmentRepository.findByTrackId(track.getId());
        for (MentorAssignment assignment : assignments) {
            mentorAssignmentRepository.delete(assignment);
        }
        return ResponseEntity.ok(Map.of("message", "Mentors removed from track successfully."));
    }

    @GetMapping("/teams/{teamId}/mentors")
    public ResponseEntity<?> getTeamMentors(@PathVariable Integer teamId) {
        List<TeamRoundTrack> trts = teamRoundTrackRepository.findByTeamId(teamId);
        List<Map<String, Object>> resp = new java.util.ArrayList<>();
        for (TeamRoundTrack trt : trts) {
            Track track = trt.getTrack();
            if (track != null) {
                List<MentorAssignment> assignments = mentorAssignmentRepository.findByTrackId(track.getId());
                for (MentorAssignment assignment : assignments) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("roundId", track.getRound() != null ? track.getRound().getId() : null);
                    item.put("mentorId", assignment.getMentor().getId());
                    item.put("mentorName", assignment.getMentor().getFullName());
                    item.put("trackId", track.getId());
                    item.put("trackName", track.getName());
                    resp.add(item);
                }
            }
        }
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/teams/hackathons/{id}/orphans")
    public ResponseEntity<?> getOrphans(@PathVariable Integer id) {
        return ResponseEntity.ok(teamService.getOrphanUsers(id));
    }

    @GetMapping("/teams/hackathons/{id}/incomplete-teams")
    public ResponseEntity<?> getIncompleteTeams(@PathVariable Integer id) {
        return ResponseEntity.ok(teamService.getIncompleteTeams(id));
    }

    @PostMapping("/teams/{teamId}/confirm-formation")
    public ResponseEntity<?> confirmFormation(@PathVariable Integer teamId, @AuthenticationPrincipal User user) {
        teamService.confirmTeamFormation(teamId, user.getId());
        return ResponseEntity.ok(Map.of("message", "Team formation confirmed."));
    }

    @PatchMapping("/teams/{teamId}/status")
    public ResponseEntity<?> updateTeamStatus(@PathVariable Integer teamId, @RequestBody Map<String, String> req, @AuthenticationPrincipal User user) {
        teamService.updateTeamStatus(teamId, req.get("status"), user);
        return ResponseEntity.ok(Map.of("message", "Team status updated."));
    }

    @PostMapping("/teams/bulk-approve")
    public ResponseEntity<?> bulkApproveTeams(@RequestBody Map<String, List<Integer>> req, @AuthenticationPrincipal User coordinator) {
        teamService.bulkApproveTeams(req.get("teamIds"), coordinator);
        return ResponseEntity.ok(Map.of("message", "Teams bulk approved."));
    }

    @PostMapping("/teams/hackathons/{id}/matchmaking")
    public ResponseEntity<?> runMatchmaking(@PathVariable Integer id, @AuthenticationPrincipal User coordinator) {
        teamService.runMatchmaking(id, coordinator);
        return ResponseEntity.ok(Map.of("message", "Matchmaking completed successfully."));
    }

    @PostMapping("/teams/admin-create")
    public ResponseEntity<?> adminCreateTeam(@RequestBody AdminCreateTeamRequest req,
            @AuthenticationPrincipal User coordinator) {
        Team t = teamService.adminCreateTeam(req.getName(), req.getLeaderId(), req.getHackathonId());
        return ResponseEntity.ok(t);
    }

    @PostMapping("/teams/{id}/admin-add-member")
    public ResponseEntity<?> adminAddMember(@PathVariable Integer id,
            @RequestBody AdminAddMemberRequest req) {
        teamService.adminAddMember(id, req.getUserId());
        return ResponseEntity.ok(Map.of("message", "Member added by admin."));
    }

    @PostMapping("/teams/{id}/admin-merge")
    public ResponseEntity<?> adminMergeTeam(@PathVariable Integer id,
            @RequestBody AdminMergeTeamRequest req) {
        teamService.mergeTeams(req.getSourceTeamId(), req.getTargetTeamId());
        return ResponseEntity.ok(Map.of("message", "Teams merged successfully."));
    }
}
