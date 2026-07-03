package hackthon.mangaement.hackathon.repository;

import hackthon.mangaement.hackathon.model.organization.CalibrationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CalibrationSessionRepository extends JpaRepository<CalibrationSession, Integer> {
    List<CalibrationSession> findByRoundId(Integer roundId);
}
