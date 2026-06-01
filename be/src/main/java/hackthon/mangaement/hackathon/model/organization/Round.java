package hackthon.mangaement.hackathon.model.organization;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import hackthon.mangaement.hackathon.model.User.User;

@Entity
@Table(name = "rounds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Round {

    public enum RoundType {
        PRELIMINARY, SEMIFINAL, FINAL
    }

    public enum LateSubmissionPolicy {
        ALLOW_LATE_PENDING, HARD_LOCK
    }

    public enum TiebreakRule {
        PENALTY_SCORE, SUBMISSION_TIME, COORDINATOR_DECISION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private Hackathon hackathon;

    @Column(nullable = false)
    private String name;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(name = "is_final", nullable = false)
    private Boolean isFinal = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "round_type", nullable = false)
    private RoundType roundType = RoundType.PRELIMINARY;

    @Column(name = "coding_duration_hours")
    private Integer codingDurationHours;

    @Column(name = "submission_open")
    private LocalDateTime submissionOpen;

    @Column(name = "submission_deadline", nullable = false)
    private LocalDateTime submissionDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "late_submission_policy", nullable = false)
    private LateSubmissionPolicy lateSubmissionPolicy = LateSubmissionPolicy.ALLOW_LATE_PENDING;

    @Column(name = "problem_statement_url")
    private String problemStatementUrl;

    @Column(name = "problem_released_at")
    private LocalDateTime problemReleasedAt;

    @Column(name = "top_n_advance")
    private Integer topNAdvance;

    @Column(name = "min_teams_final")
    private Integer minTeamsFinal;

    @Column(name = "wildcard_enabled", nullable = false)
    private Boolean wildcardEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "tiebreak_rule")
    private TiebreakRule tiebreakRule = TiebreakRule.PENALTY_SCORE;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(name = "scoring_locked", nullable = false)
    private Boolean scoringLocked = false;

    @Column(name = "scoring_locked_at")
    private LocalDateTime scoringLockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scoring_locked_by")
    private User scoringLockedBy;

    @Column(name = "force_locked", nullable = false)
    private Boolean forceLocked = false;

    @Column(name = "force_lock_reason", columnDefinition = "TEXT")
    private String forceLockReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
