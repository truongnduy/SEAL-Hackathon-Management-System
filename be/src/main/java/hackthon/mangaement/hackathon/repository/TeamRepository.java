package hackthon.mangaement.hackathon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import hackthon.mangaement.hackathon.model.Team.Team;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Integer> {
    Optional<Team> findByTeamName(String teamName);

    List<Team> findByHackathonId(Integer hackathonId);

    @Query("SELECT t FROM Team t WHERE t.hackathon.id = :hackathonId AND (SELECT COUNT(tm) FROM TeamMember tm WHERE tm.team.id = t.id) < 3")
    List<Team> findIncompleteTeams(@Param("hackathonId") Integer hackathonId);
}
