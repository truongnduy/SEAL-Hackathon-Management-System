package hackthon.mangaement.hackathon.model.organization;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import hackthon.mangaement.hackathon.model.Team.Team;
import hackthon.mangaement.hackathon.model.User.User;

@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    public enum Status {
        SUBMITTED, LATE, LATE_PENDING, LATE_APPROVED, REJECTED, ACCEPTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id")
    private Track track;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id")
    private Round round;

    @Column(name = "repo_url")
    private String repoUrl;

    @Column(name = "demo_url")
    private String demoUrl;

    @Column(name = "report_url")
    private String reportUrl;

    @Column(name = "slide_url")
    private String slideUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.SUBMITTED;

    @Column(name = "is_late", nullable = false)
    private Boolean isLate = false;

    @Column(name = "late_reason", columnDefinition = "TEXT")
    private String lateReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();
}
