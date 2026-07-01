package hackthon.mangaement.hackathon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import hackthon.mangaement.hackathon.model.User.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.role = 'STUDENT' AND u.id NOT IN " +
           "(SELECT tm.user.id FROM TeamMember tm WHERE tm.team.hackathon.id = :hackathonId)")
    List<User> findOrphansByHackathon(@Param("hackathonId") Integer hackathonId);
}
