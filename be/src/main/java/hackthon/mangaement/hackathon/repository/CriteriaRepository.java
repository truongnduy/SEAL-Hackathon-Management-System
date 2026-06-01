package hackthon.mangaement.hackathon.repository;

import hackthon.mangaement.hackathon.model.organization.Criteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CriteriaRepository extends JpaRepository<Criteria, Integer> {
    List<Criteria> findByTrackIdOrderByDisplayOrderAsc(Integer trackId);
    List<Criteria> findByRoundIdOrderByDisplayOrderAsc(Integer roundId);
}
