package hackthon.mangaement.hackathon.repository;

import hackthon.mangaement.hackathon.model.Mentor.MentorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MentorAssignmentRepository extends JpaRepository<MentorAssignment, Integer> {
    List<MentorAssignment> findByTrackId(Integer trackId);
    Optional<MentorAssignment> findByMentorIdAndTrackId(Integer mentorId, Integer trackId);
    boolean existsByMentorId(Integer mentorId);
}
