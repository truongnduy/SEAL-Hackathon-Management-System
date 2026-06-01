package hackthon.mangaement.hackathon.repository;

import hackthon.mangaement.hackathon.model.User.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {
    List<AuditLog> findByTargetTableAndTargetIdOrderByCreatedAtDesc(String targetTable, Integer targetId);
}
