package hackthon.mangaement.hackathon.controller;

import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.model.Chapter.Chapter;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.repository.ChapterRepository;
import hackthon.mangaement.hackathon.repository.UserRepository;
import hackthon.mangaement.hackathon.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private hackthon.mangaement.hackathon.repository.OAuthAccountRepository oAuthAccountRepository;

    @GetMapping("/users/me/oauth-providers")
    public ResponseEntity<?> getMyOAuthProviders(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        List<String> providers = oAuthAccountRepository.findByUser(user).stream()
                .map(acc -> acc.getProvider())
                .collect(Collectors.toList());
        return ResponseEntity.ok(providers);
    }

    @GetMapping("/users/me")
    public ResponseEntity<?> getMe(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        // Return re-fetched user to get fresh details
        User freshUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(freshUser);
    }

    @PatchMapping("/users/me")
    public ResponseEntity<?> patchMe(@RequestBody Map<String, Object> req,
                                     @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }

        User dbUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (req.containsKey("fullName")) {
            dbUser.setFullName((String) req.get("fullName"));
        }
        if (req.containsKey("userType")) {
            dbUser.setUserType(User.UserType.valueOf((String) req.get("userType")));
        }
        if (req.containsKey("studentCode")) {
            dbUser.setStudentCode((String) req.get("studentCode"));
        }
        if (req.containsKey("institution")) {
            dbUser.setInstitution((String) req.get("institution"));
        }
        if (req.containsKey("phone")) {
            dbUser.setPhone((String) req.get("phone"));
        }
        if (req.containsKey("chapterId") && req.get("chapterId") != null) {
            Integer chapterId = ((Number) req.get("chapterId")).intValue();
            Chapter chapter = chapterRepository.findById(chapterId)
                    .orElseThrow(() -> new ResourceNotFoundException("Chapter not found: " + chapterId));
            dbUser.setChapter(chapter);
        }

        dbUser.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(dbUser);
        return ResponseEntity.ok(savedUser);
    }

    @PostMapping("/users/me/student-card")
    public ResponseEntity<?> uploadStudentCard(@RequestParam("file") MultipartFile file,
                                               @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        User dbUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        try {
            dbUser.setStudentCardData(file.getBytes());
        } catch (java.io.IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to read file: " + e.getMessage()));
        }
        dbUser.setStudentCardContentType(file.getContentType());
        dbUser.setAvatarUrl("/api/users/" + dbUser.getId() + "/student-card");
        dbUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(dbUser);

        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Student card uploaded successfully.");
        resp.put("fileUrl", dbUser.getAvatarUrl());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/users/{userId}/student-card")
    public ResponseEntity<byte[]> getStudentCard(@PathVariable Integer userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (u.getStudentCardData() == null) {
            return ResponseEntity.notFound().build();
        }
        String contentType = u.getStudentCardContentType();
        if (contentType == null) {
            contentType = "image/jpeg";
        }
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .body(u.getStudentCardData());
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(@RequestParam(required = false) String role,
                                      @RequestParam(required = false) String status) {
        List<User> users = userRepository.findAll();
        if (role != null && !role.trim().isEmpty()) {
            User.Role r = User.Role.valueOf(role);
            users = users.stream().filter(u -> u.getRole() == r).collect(Collectors.toList());
        }
        if (status != null && !status.trim().isEmpty()) {
            User.Status s = User.Status.valueOf(status);
            users = users.stream().filter(u -> u.getStatus() == s).collect(Collectors.toList());
        }
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserDetail(@PathVariable Integer userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return ResponseEntity.ok(u);
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Integer userId,
                                              @RequestBody Map<String, Object> req,
                                              @AuthenticationPrincipal User coordinator,
                                              HttpServletRequest servletRequest) {
        if (coordinator == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        String statusStr = (String) req.get("status");
        User.Status status = User.Status.valueOf(statusStr);
        boolean approve = (status == User.Status.APPROVED);
        String reason = (String) req.get("rejectionReason");

        authService.approveAccount(userId, approve, reason, coordinator, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of("message", "User status updated to " + statusStr));
    }

    @PostMapping("/users/temp-judges")
    public ResponseEntity<?> createTempJudge(@RequestBody Map<String, String> req,
                                             @AuthenticationPrincipal User coordinator,
                                             HttpServletRequest servletRequest) {
        if (coordinator == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        String name = req.get("name") != null ? req.get("name") : req.get("fullName");
        User judge = authService.createTempJudgeAccount(
                name,
                req.get("email"),
                coordinator,
                servletRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(judge);
    }

    @GetMapping("/users/temp-judges")
    public ResponseEntity<?> getTempJudges() {
        List<User> list = userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsTempAccount()) && u.getRole() == User.Role.JUDGE)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/invitations/{invitationId}/resend")
    public ResponseEntity<?> resendInvitation(@PathVariable Integer invitationId) {
        return ResponseEntity.ok(Map.of("message", "Invitation resent successfully."));
    }
}
