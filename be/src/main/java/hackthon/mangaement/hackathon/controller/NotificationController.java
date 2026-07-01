package hackthon.mangaement.hackathon.controller;

import hackthon.mangaement.hackathon.model.Notification.Notification;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        List<Notification> list = notificationService.getNotificationsForUser(user.getId());
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Integer id, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        notificationService.markAsRead(id);
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Notification marked as read.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/notifications/read-all")
    public ResponseEntity<?> markAllAsRead(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        List<Notification> unread = notificationService.getUnreadNotificationsForUser(user.getId());
        for (Notification n : unread) {
            notificationService.markAsRead(n.getId());
        }
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "All notifications marked as read.");
        return ResponseEntity.ok(resp);
    }
}
