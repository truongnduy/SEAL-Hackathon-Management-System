package hackthon.mangaement.hackathon.model.organization;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import hackthon.mangaement.hackathon.model.Team.Team;
import hackthon.mangaement.hackathon.model.User.User;

@Entity
@Table(name = "prizes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prize {

    public enum PrizeRank {
        FIRST, SECOND, THIRD, HONORABLE, SPECIAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id")
    private Track track;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "prize_name", nullable = false)
    private String prizeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "prize_rank")
    private PrizeRank prizeRank;

    @Column(name = "prize_value")
    private String prizeValue;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "awarded_at", nullable = false)
    private LocalDateTime awardedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "awarded_by")
    private User awardedBy;
}
