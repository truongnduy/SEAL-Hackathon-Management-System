package hackthon.mangaement.hackathon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import hackthon.mangaement.hackathon.model.organization.Submission;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Integer> {
    Optional<Submission> findByTeamIdAndTrackId(Integer teamId, Integer trackId);
    Optional<Submission> findByTeamIdAndRoundId(Integer teamId, Integer roundId);
    List<Submission> findByTrackId(Integer trackId);
    List<Submission> findByRoundId(Integer roundId);
    List<Submission> findByStatus(Submission.Status status);
}
