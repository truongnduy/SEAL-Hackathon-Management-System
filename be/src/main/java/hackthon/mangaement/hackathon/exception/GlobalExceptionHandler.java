package hackthon.mangaement.hackathon.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<?> handleConflict(ConflictException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<?> handleBusinessRule(BusinessRuleException ex) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrity(DataIntegrityViolationException ex) {
        Throwable cause = ex.getRootCause();
        if (cause instanceof SQLException) {
            SQLException sqlException = (SQLException) cause;
            String state = sqlException.getSQLState();
            if ("45000".equals(state)) {
                return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, sqlException.getMessage());
            }
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Database constraint violation: " + ex.getMessage());
    }

    @ExceptionHandler(OAuthException.class)
    public ResponseEntity<?> handleOAuth(OAuthException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> error = new HashMap<>();
        error.put("code", ex.getCode());
        error.put("message", ex.getMessage());
        response.put("error", error);
        return new ResponseEntity<>(response, ex.getStatus());
    }

    private ResponseEntity<Map<String, String>> buildResponse(HttpStatus status, String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", status.getReasonPhrase());
        response.put("message", message);
        return new ResponseEntity<>(response, status);
    }
}
