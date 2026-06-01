package hackthon.mangaement.hackathon.repository;

import hackthon.mangaement.hackathon.model.Team.TeamMember;
import hackthon.mangaement.hackathon.model.Team.TeamMemberId;
import hackthon.mangaement.hackathon.model.Team.TeamMember.Status;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMemberId> {
    List<TeamMember> findByTeamId(Integer teamId);
    List<TeamMember> findByUserId(Integer userId);
    List<TeamMember> findByTeamIdAndStatus(Integer teamId, Status status);
    Optional<TeamMember> findByTeamIdAndUserId(Integer teamId, Integer userId);
    boolean existsByUserIdAndStatus(Integer userId, Status status);
}
