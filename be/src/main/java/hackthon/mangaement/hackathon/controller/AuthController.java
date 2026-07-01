package hackthon.mangaement.hackathon.controller;

import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.exception.OAuthException;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.repository.UserRepository;
import hackthon.mangaement.hackathon.service.AuthService;
import hackthon.mangaement.hackathon.service.OAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OAuthService oAuthService;

    @PostMapping("/register")
    public ResponseEntity<?> signup(@RequestBody Map<String, Object> req) {
        User user = authService.signup(
                (String) req.get("fullName"),
                (String) req.get("email"),
                (String) req.get("password"),
                User.Role.valueOf((String) req.get("role")),
                User.UserType.valueOf((String) req.get("userType")),
                (String) req.get("studentCode"),
                (Integer) req.get("chapterId"),
                (String) req.get("phone"),
                (String) req.get("institution")
        );
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        String token = authService.login(email, req.get("password"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        Map<String, Object> resp = new HashMap<>();
        resp.put("accessToken", token);
        resp.put("refreshToken", token);
        resp.put("email", user.getEmail());
        resp.put("status", user.getStatus().name());
        resp.put("role", user.getRole().name());
        resp.put("userId", user.getId());

        Map<String, Object> userObj = new HashMap<>();
        userObj.put("id", user.getId());
        userObj.put("userId", user.getId());
        userObj.put("email", user.getEmail());
        userObj.put("status", user.getStatus().name());
        userObj.put("role", user.getRole().name());
        resp.put("user", userObj);

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/oauth/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> req) {
        String tokenValue = req.get("tokenValue");
        String existingAccountPassword = req.get("existingAccountPassword");
        Map<String, Object> googleInfo = oAuthService.verifyGoogleToken(tokenValue);
        String sub = (String) googleInfo.get("sub");
        String email = (String) googleInfo.get("email");
        String name = (String) googleInfo.get("name");

        Map<String, Object> resp = oAuthService.loginOrRegisterOAuth(
                "google", sub, email, name, existingAccountPassword, null, null);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/oauth/github/code")
    public ResponseEntity<?> githubLogin(@RequestBody Map<String, String> req) {
        String code = req.get("code");
        String redirectUri = req.get("redirectUri");
        String existingAccountPassword = req.get("existingAccountPassword");
        Map<String, Object> githubInfo = oAuthService.verifyGithubCode(code, redirectUri);
        String id = (String) githubInfo.get("id");
        String email = (String) githubInfo.get("email");
        String name = (String) githubInfo.get("name");
        String accessToken = (String) githubInfo.get("accessToken");
        String refreshToken = (String) githubInfo.get("refreshToken");

        Map<String, Object> resp = oAuthService.loginOrRegisterOAuth(
                "github", id, email, name, existingAccountPassword, accessToken, refreshToken);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/oauth/google/link")
    public ResponseEntity<?> googleLink(@RequestBody Map<String, String> req,
                                        @AuthenticationPrincipal User user) {
        if (user == null) {
            throw new OAuthException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Vui lòng đăng nhập.");
        }
        String idToken = req.get("idToken");
        oAuthService.linkGoogle(user, idToken);
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Liên kết Google thành công.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/oauth/github/link/code")
    public ResponseEntity<?> githubLink(@RequestBody Map<String, String> req,
                                        @AuthenticationPrincipal User user) {
        if (user == null) {
            throw new OAuthException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Vui lòng đăng nhập.");
        }
        String code = req.get("code");
        String redirectUri = req.get("redirectUri");
        oAuthService.linkGithub(user, code, redirectUri);
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Liên kết GitHub thành công.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/oauth/google/unlink")
    public ResponseEntity<?> googleUnlink(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new OAuthException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Vui lòng đăng nhập.");
        }
        oAuthService.unlinkGoogle(user);
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Đã gỡ liên kết Google.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/oauth/github/unlink")
    public ResponseEntity<?> githubUnlink(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new OAuthException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Vui lòng đăng nhập.");
        }
        oAuthService.unlinkGithub(user);
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Đã gỡ liên kết GitHub.");
        return ResponseEntity.ok(resp);
    }


    @PostMapping("/approve/{userId}")
    public ResponseEntity<?> approveAccount(@PathVariable Integer userId,
                                            @RequestBody Map<String, Object> req,
                                            @AuthenticationPrincipal User coordinator,
                                            HttpServletRequest servletRequest) {
        boolean approve = (Boolean) req.get("approve");
        String reason = (String) req.get("reason");
        authService.approveAccount(userId, approve, reason, coordinator, servletRequest.getRemoteAddr());
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "User account status updated successfully.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/judge/temp")
    public ResponseEntity<?> createTempJudge(@RequestBody Map<String, String> req,
                                             @AuthenticationPrincipal User coordinator,
                                             HttpServletRequest servletRequest) {
        User judge = authService.createTempJudgeAccount(
                req.get("fullName"),
                req.get("email"),
                coordinator,
                servletRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(judge);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) Map<String, String> req) {
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Logged out successfully.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> req,
                                            @AuthenticationPrincipal User user) {
        // Tạm thời trả về OK để Frontend chạy được luồng giao diện
        // Cần implement logic đổi mật khẩu thật (kiểm tra password cũ, hash password mới, lưu DB)
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Change password simulated successfully.");
        return ResponseEntity.ok(resp);
    }
}
