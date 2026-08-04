package hackthon.mangaement.hackathon.repository;

import hackthon.mangaement.hackathon.model.organization.SubmissionScoringConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionScoringConfirmationRepository extends JpaRepository<SubmissionScoringConfirmation, Integer> {
    Optional<SubmissionScoringConfirmation> findBySubmissionIdAndJudgeId(Integer submissionId, Integer judgeId);
    List<SubmissionScoringConfirmation> findBySubmissionId(Integer submissionId);
    boolean existsBySubmissionIdAndJudgeId(Integer submissionId, Integer judgeId);
    long countBySubmissionId(Integer submissionId);
}
