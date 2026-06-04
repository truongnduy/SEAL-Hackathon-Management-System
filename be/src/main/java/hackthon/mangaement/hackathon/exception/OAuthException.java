package hackthon.mangaement.hackathon.exception;

import org.springframework.http.HttpStatus;

public class OAuthException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public OAuthException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
