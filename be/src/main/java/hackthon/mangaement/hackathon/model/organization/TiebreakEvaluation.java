package hackthon.mangaement.hackathon.model.organization;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import hackthon.mangaement.hackathon.model.Team.Team;
import hackthon.mangaement.hackathon.model.User.User;

@Entity
@Table(name = "tiebreak_evaluations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TiebreakEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "judge_id", nullable = false)
    private User judge;

    @Column(name = "penalty_score", nullable = false)
    private Double penaltyScore = 0.0;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt = LocalDateTime.now();
}
