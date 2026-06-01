package hackthon.mangaement.hackathon.model.organization;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "criteria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Criteria {

    public enum CriteriaType {
        TECHNICAL, SOFT_SKILL, PENALTY
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id")
    private Track track;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id")
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_criteria_id")
    private Criteria sourceCriteria;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CriteriaType type;

    @Column(nullable = false)
    private Double weight;

    @Column(name = "max_score", nullable = false)
    private Integer maxScore = 10;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "rubric_url")
    private String rubricUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
}
