package hackthon.mangaement.hackathon.model.Notification;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import hackthon.mangaement.hackathon.model.User.User;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String type; // e.g. ACCOUNT_APPROVED, TEAM_APPROVED, ROUND_STARTED

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "reference_id")
    private Integer referenceId;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
