package hackthon.mangaement.hackathon.model.Team;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import hackthon.mangaement.hackathon.model.User.User;

@Entity
@Table(name = "team_members")
@IdClass(TeamMemberId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMember {

    public enum RoleInTeam {
        LEADER, MEMBER
    }

    public enum Status {
        PENDING, ACCEPTED, REJECTED, LEFT
    }

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_in_team", nullable = false)
    private RoleInTeam roleInTeam = RoleInTeam.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;
}
