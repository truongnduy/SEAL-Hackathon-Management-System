package hackthon.mangaement.hackathon.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import hackthon.mangaement.hackathon.model.User.AuditLog;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public void logAction(User user, String action, String targetTable, Integer targetId, Object detail, String ipAddress) {
        String detailJson = null;
        if (detail != null) {
            try {
                detailJson = objectMapper.writeValueAsString(detail);
            } catch (Exception e) {
                detailJson = "{\"error\":\"Could not serialize detail: " + e.getMessage() + "\"}";
            }
        }
        
        AuditLog log = AuditLog.builder()
                .user(user)
                .action(action)
                .targetTable(targetTable)
                .targetId(targetId)
                .detail(detailJson)
                .ipAddress(ipAddress)
                .build();
        auditLogRepository.save(log);
    }
}
