package hackthon.mangaement.hackathon.model.organization;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

import hackthon.mangaement.hackathon.model.User.User;

@Entity
@Table(name = "hackathons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hackathon {

    public enum Status {
        DRAFT, ONGOING, PENDING_CONFIRM, FINISHED
    }

    public enum Season {
        Spring, Summer, Fall, Winter
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Season season;

    @Column(nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String rules;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Column(name = "registration_start")
    private LocalDate registrationStart;

    @Column(name = "registration_end")
    private LocalDate registrationEnd;

    @Column(name = "event_start")
    private LocalDate eventStart;

    @Column(name = "event_end")
    private LocalDate eventEnd;

    @Column(name = "wildcard_enabled", nullable = false)
    private Boolean wildcardEnabled = false;

    @Column(name = "individual_ranking_enabled", nullable = false)
    private Boolean individualRankingEnabled = false;

    @Column(name = "chapter_scoring_formula", columnDefinition = "TEXT")
    private String chapterScoringFormula; // JSON configuration

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
