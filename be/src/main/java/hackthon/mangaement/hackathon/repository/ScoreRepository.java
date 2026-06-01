package hackthon.mangaement.hackathon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import hackthon.mangaement.hackathon.model.organization.Score;
import hackthon.mangaement.hackathon.model.organization.Score.ScoreType;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Integer> {
    List<Score> findBySubmissionId(Integer submissionId);
    List<Score> findBySubmissionIdAndScoreType(Integer submissionId, ScoreType scoreType);
    Optional<Score> findBySubmissionIdAndJudgeIdAndCriterionIdAndScoreType(
            Integer submissionId, Integer judgeId, Integer criterionId, ScoreType scoreType);
    List<Score> findByJudgeId(Integer judgeId);
}
