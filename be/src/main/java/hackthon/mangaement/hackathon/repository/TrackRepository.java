package hackthon.mangaement.hackathon.repository;

import hackthon.mangaement.hackathon.model.organization.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TrackRepository extends JpaRepository<Track, Integer> {
    List<Track> findByRoundIdOrderBySequenceOrderAsc(Integer roundId);
}
