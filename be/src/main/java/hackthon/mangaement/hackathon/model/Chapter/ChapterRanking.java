package hackthon.mangaement.hackathon.model.Chapter;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import hackthon.mangaement.hackathon.model.organization.Hackathon;

@Entity
@Table(name = "chapter_rankings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChapterRanking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private Hackathon hackathon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(name = "best_team_score", nullable = false)
    private Double bestTeamScore = 0.0;

    @Column(name = "total_score", nullable = false)
    private Double totalScore = 0.0;

    @Column(name = "`rank`")
    private Integer rank;

    @Column(name = "teams_participated", nullable = false)
    private Integer teamsParticipated = 0;

    @Column(name = "prizes_won", nullable = false)
    private Integer prizesWon = 0;

    @Column(name = "formula_snapshot", columnDefinition = "TEXT")
    private String formulaSnapshot; // JSON string

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt = LocalDateTime.now();
}
