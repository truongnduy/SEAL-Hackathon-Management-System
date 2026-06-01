package hackthon.mangaement.hackathon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import hackthon.mangaement.hackathon.model.Team.TeamRoundTrack;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRoundTrackRepository extends JpaRepository<TeamRoundTrack, Integer> {
    List<TeamRoundTrack> findByTeamId(Integer teamId);
    List<TeamRoundTrack> findByTrackId(Integer trackId);
    Optional<TeamRoundTrack> findByTeamIdAndTrackId(Integer teamId, Integer trackId);
}
