package hackthon.mangaement.hackathon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import hackthon.mangaement.hackathon.model.Notification.Notification;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByUserIdOrderBySentAtDesc(Integer userId);
    List<Notification> findByUserIdAndIsReadFalseOrderBySentAtDesc(Integer userId);
}
