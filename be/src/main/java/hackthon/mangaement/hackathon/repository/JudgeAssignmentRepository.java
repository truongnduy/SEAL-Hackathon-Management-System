package hackthon.mangaement.hackathon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import hackthon.mangaement.hackathon.model.Judge.JudgeAssignment;

import java.util.List;
import java.util.Optional;

@Repository
public interface JudgeAssignmentRepository extends JpaRepository<JudgeAssignment, Integer> {
    List<JudgeAssignment> findByTrackId(Integer trackId);
    List<JudgeAssignment> findByRoundId(Integer roundId);
    Optional<JudgeAssignment> findByJudgeIdAndTrackId(Integer judgeId, Integer trackId);
    Optional<JudgeAssignment> findByJudgeIdAndRoundId(Integer judgeId, Integer roundId);
}
