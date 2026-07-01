package hackthon.mangaement.hackathon.controller;

import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.service.MeService;
import hackthon.mangaement.hackathon.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    @Autowired
    private MeService meService;

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/hackathons/browse")
    public ResponseEntity<?> browseHackathons(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(meService.getBrowseableHackathons(user));
    }

    @PostMapping("/hackathons/{id}/register")
    public ResponseEntity<?> registerHackathon(@PathVariable Integer id, @AuthenticationPrincipal User user) {
        meService.registerForHackathon(id, user);
        return ResponseEntity.ok(Map.of("message", "Registered to hackathon successfully."));
    }

    @GetMapping("/judge/submissions")
    public ResponseEntity<?> getJudgeSubmissions(@AuthenticationPrincipal User judge) {
        return ResponseEntity.ok(meService.getJudgeSubmissions(judge));
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> getMyNotifications(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.getNotificationsForUser(user.getId()));
    }

    @PatchMapping("/notifications/read")
    public ResponseEntity<?> markNotificationsAsRead(@RequestBody(required = false) Map<String, Object> req, @AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(Map.of("message", "Notifications marked as read."));
    }
}
