package hackthon.mangaement.hackathon.service;

import hackthon.mangaement.hackathon.model.Notification.Notification;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

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
        return notificationRepository.save(notification);
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
}
