package hackthon.mangaement.hackathon.service;

import hackthon.mangaement.hackathon.exception.ConflictException;
import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.model.Chapter.Chapter;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.repository.ChapterRepository;
import hackthon.mangaement.hackathon.repository.UserRepository;
import hackthon.mangaement.hackathon.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private JavaMailSender mailSender;

    public User signup(String fullName, String email, String password, User.Role role, User.UserType userType,
                       String studentCode, Integer chapterId, String phone, String institution) {
        
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already exists: " + email);
        }

        Chapter chapter = null;
        if (chapterId != null) {
            chapter = chapterRepository.findById(chapterId)
                    .orElseThrow(() -> new ResourceNotFoundException("Chapter not found with ID: " + chapterId));
        }

        User.Status status = User.Status.PENDING;
        // Auto-approve internal students with @fpt.edu.vn emails
        if (userType == User.UserType.INTERNAL && email.endsWith("@fpt.edu.vn")) {
            status = User.Status.APPROVED;
        }

        User user = User.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .userType(userType)
                .studentCode(studentCode)
                .chapter(chapter)
                .phone(phone)
                .institution(institution)
                .status(status)
                .isTempAccount(false)
                .isDeptHead(false)
                .build();

        User savedUser = userRepository.save(savedUserInstance(user));

        if (status == User.Status.APPROVED) {
            notificationService.sendNotification(savedUser, "ACCOUNT_APPROVED", "Account Approved",
                    "Welcome! Your internal account has been automatically approved.", "users", savedUser.getId());
        }

        return savedUser;
    }

    private User savedUserInstance(User u) {
        return u;
    }

    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password."));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResourceNotFoundException("Invalid email or password.");
        }

        if (user.getStatus() != User.Status.APPROVED) {
            throw new ConflictException("Your account is currently " + user.getStatus().name() + ". Please wait for coordinator approval.");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return jwtTokenUtil.generateToken(user.getEmail(), user.getRole().name());
    }

    public void approveAccount(Integer userId, boolean approve, String reason, User coordinator, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        User.Status oldStatus = user.getStatus();
        User.Status newStatus = approve ? User.Status.APPROVED : User.Status.REJECTED;
        user.setStatus(newStatus);
        user.setRejectionReason(approve ? null : reason);
        userRepository.save(user);

        // Audit Logging
        Map<String, Object> detail = new HashMap<>();
        detail.put("userId", userId);
        detail.put("oldStatus", oldStatus.name());
        detail.put("newStatus", newStatus.name());
        detail.put("reason", reason);
        auditLogService.logAction(coordinator, approve ? "ACCOUNT_APPROVE" : "ACCOUNT_REJECT", "users", userId, detail, ipAddress);

        // Notifications
        String title = approve ? "Account Approved" : "Account Rejected";
        String body = approve ? "Congratulations! Your account has been approved by the Coordinator." 
                             : "Sorry, your account was rejected. Reason: " + reason;
        notificationService.sendNotification(user, approve ? "ACCOUNT_APPROVED" : "ACCOUNT_REJECTED", title, body, "users", userId);

        // Send Email
        String subject = approve ? "Tài khoản SEAL Hackathon của bạn đã được phê duyệt" 
                                 : "Tài khoản SEAL Hackathon của bạn đã bị từ chối";
        String emailText = approve ? "Xin chúc mừng! Tài khoản đăng ký tham gia SEAL Hackathon của bạn đã được phê duyệt bởi Điều phối viên.\nBây giờ bạn có thể đăng nhập vào hệ thống."
                                 : "Chúng tôi rất tiếc phải thông báo rằng tài khoản của bạn đã bị từ chối.\nLý do: " + reason;
        sendEmail(user.getEmail(), subject, emailText);
    }

    public User createTempJudgeAccount(String fullName, String email, User coordinator, String ipAddress) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already exists: " + email);
        }

        String rawPassword = UUID.randomUUID().toString().substring(0, 8); // random password
        User judge = User.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(User.Role.JUDGE)
                .userType(User.UserType.EXTERNAL)
                .isTempAccount(true)
                .isDeptHead(false)
                .status(User.Status.APPROVED)
                .emailVerifiedAt(LocalDateTime.now())
                .build();

        User savedJudge = userRepository.save(judge);

        // Log coordinator action
        Map<String, Object> detail = new HashMap<>();
        detail.put("tempJudgeId", savedJudge.getId());
        detail.put("email", email);
        detail.put("fullName", fullName);
        auditLogService.logAction(coordinator, "CREATE_TEMP_JUDGE_ACCOUNT", "users", savedJudge.getId(), detail, ipAddress);

        // In a real system, send email here. In this system, we can send a notification/log password for debug/mock.
        System.out.println("Temp Judge Account Created: email=" + email + ", password=" + rawPassword);
        
        // Send Email
        String subject = "Tài khoản Giám khảo khách mời SEAL Hackathon";
        String emailText = "Xin chào " + fullName + ",\n\nBạn đã được mời làm Giám khảo khách mời cho SEAL Hackathon.\n"
                    + "Thông tin đăng nhập của bạn như sau:\n"
                    + "Email: " + email + "\n"
                    + "Mật khẩu: " + rawPassword + "\n\n"
                    + "Vui lòng đăng nhập và thay đổi mật khẩu sau khi truy cập hệ thống.";
        sendEmail(email, subject, emailText);
        
        return savedJudge;
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom("no-reply@sealhackathon.com");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
        }
    }
}
