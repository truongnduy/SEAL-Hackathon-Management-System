package hackthon.mangaement.hackathon.service;

import hackthon.mangaement.hackathon.model.Notification.Notification;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public Notification sendNotification(User user, String type, String title, String body, String refType, Integer refId) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .referenceType(refType)
                .referenceId(refId)
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();
        Notification saved = notificationRepository.save(notification);

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", saved.getId());
            payload.put("type", saved.getType());
            payload.put("title", saved.getTitle());
            payload.put("body", saved.getBody());
            payload.put("referenceType", saved.getReferenceType());
            payload.put("referenceId", saved.getReferenceId());
            payload.put("isRead", saved.getIsRead());
            payload.put("sentAt", saved.getSentAt().toString());

            String json = mapper.writeValueAsString(payload);
            hackthon.mangaement.hackathon.config.WebSocketNotificationHandler.sendNotificationToUser(user.getId(), json);
        } catch (Exception e) {
            System.err.println("Failed to send WebSocket notification: " + e.getMessage());
        }

        return saved;
    }

    public List<Notification> getNotificationsForUser(Integer userId) {
        return notificationRepository.findByUserIdOrderBySentAtDesc(userId);
    }

    public List<Notification> getUnreadNotificationsForUser(Integer userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderBySentAtDesc(userId);
    }

    public void markAsRead(Integer notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            n.setReadAt(LocalDateTime.now());
            notificationRepository.save(n);
        });
    }

    public void markAllAsRead(Integer userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalseOrderBySentAtDesc(userId);
        for (Notification n : unread) {
            n.setIsRead(true);
            n.setReadAt(LocalDateTime.now());
        }
        notificationRepository.saveAll(unread);
    }
}
