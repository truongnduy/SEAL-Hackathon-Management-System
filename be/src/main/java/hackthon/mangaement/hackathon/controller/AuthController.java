package hackthon.mangaement.hackathon.controller;

import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/signup")
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
        String token = authService.login(req.get("email"), req.get("password"));
        Map<String, String> resp = new HashMap<>();
        resp.put("token", token);
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
}
