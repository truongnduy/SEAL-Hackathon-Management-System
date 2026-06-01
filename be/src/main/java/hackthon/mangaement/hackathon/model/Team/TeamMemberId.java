package hackthon.mangaement.hackathon.model.Team;

import java.io.Serializable;
import java.util.Objects;

public class TeamMemberId implements Serializable {
    private Integer team;
    private Integer user;

    public TeamMemberId() {}

    public TeamMemberId(Integer team, Integer user) {
        this.team = team;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamMemberId that = (TeamMemberId) o;
        return Objects.equals(team, that.team) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(team, user);
    }

    public Integer getTeam() { return team; }
    public void setTeam(Integer team) { this.team = team; }
    public Integer getUser() { return user; }
    public void setUser(Integer user) { this.user = user; }
}
