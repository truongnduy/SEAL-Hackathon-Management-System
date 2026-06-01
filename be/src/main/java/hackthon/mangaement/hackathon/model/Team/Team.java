package hackthon.mangaement.hackathon.model.Team;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import hackthon.mangaement.hackathon.model.organization.Hackathon;
import hackthon.mangaement.hackathon.model.Chapter.Chapter;
import hackthon.mangaement.hackathon.model.User.User;

@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    public enum Status {
        PENDING, ACTIVE, ELIMINATED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private Hackathon hackathon;

    @Column(name = "team_name", nullable = false, unique = true)
    private String teamName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id", nullable = false)
    private User leader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "eliminated_at")
    private LocalDateTime eliminatedAt;

    @Column(name = "elimination_reason")
    private String eliminationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
