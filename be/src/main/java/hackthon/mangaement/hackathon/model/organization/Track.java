package hackthon.mangaement.hackathon.model.organization;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tracks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Track {

    public enum Status {
        OPEN, CLOSED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String topic;

    @Column(name = "max_teams")
    private Integer maxTeams;

    @Column(name = "max_teams_per_group")
    private Integer maxTeamsPerGroup;

    @Column(name = "min_team_size", nullable = false)
    private Integer minTeamSize = 3;

    @Column(name = "max_team_size", nullable = false)
    private Integer maxTeamSize = 5;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder = 1;
}
