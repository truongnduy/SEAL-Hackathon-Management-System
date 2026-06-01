package hackthon.mangaement.hackathon.controller;

import hackthon.mangaement.hackathon.model.Team.Team;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.service.TeamService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @PostMapping("/teams")
    public ResponseEntity<?> createTeam(@RequestBody Map<String, Object> req,
                                        @AuthenticationPrincipal User leader) {
        Team team = teamService.createTeam(
                (String) req.get("teamName"),
                leader,
                (Integer) req.get("hackathonId")
        );
        return ResponseEntity.ok(team);
    }

    @PostMapping("/teams/{id}/invite")
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
}
