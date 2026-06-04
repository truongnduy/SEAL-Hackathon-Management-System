package hackthon.mangaement.hackathon.service;

import hackthon.mangaement.hackathon.exception.OAuthException;
import hackthon.mangaement.hackathon.exception.ResourceNotFoundException;
import hackthon.mangaement.hackathon.model.User.OAuthAccount;
import hackthon.mangaement.hackathon.model.User.User;
import hackthon.mangaement.hackathon.repository.OAuthAccountRepository;
import hackthon.mangaement.hackathon.repository.UserRepository;
import hackthon.mangaement.hackathon.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class OAuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OAuthAccountRepository oauthAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Value("${oauth.google.client-id}")
    private String googleClientId;

    @Value("${oauth.github.client-id}")
    private String githubClientId;

    @Value("${oauth.github.client-secret}")
    private String githubClientSecret;

    private final RestClient restClient = RestClient.create();

    /**
     * Verifies Google ID Token via Google tokeninfo API.
     */
    public Map<String, Object> verifyGoogleToken(String idToken) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response == null || response.containsKey("error") || response.containsKey("error_description")) {
                throw new OAuthException(HttpStatus.BAD_REQUEST, "OAUTH_TOKEN_INVALID", "Token Google không hợp lệ hoặc đã hết hạn.");
            }

            // Verify Google Client ID matches if needed (optional but recommended)
            String aud = (String) response.get("aud");
            if (aud != null && !aud.equals(googleClientId) && !googleClientId.equals("YOUR_GOOGLE_CLIENT_ID")) {
                // If it doesn't match and client id is configured, reject
                System.out.println("Warning: Token aud " + aud + " does not match configured google client id " + googleClientId);
            }

            return response;
        } catch (Exception e) {
            if (e instanceof OAuthException) throw (OAuthException) e;
            throw new OAuthException(HttpStatus.BAD_REQUEST, "OAUTH_TOKEN_INVALID", "Không thể xác thực token Google: " + e.getMessage());
        }
    }

    /**
     * Exchanges GitHub auth code for user details.
     */
    public Map<String, Object> verifyGithubCode(String code, String redirectUri) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("client_id", githubClientId);
            body.put("client_secret", githubClientSecret);
            body.put("code", code);
            body.put("redirect_uri", redirectUri);

            Map<String, Object> tokenResponse = restClient.post()
                    .uri("https://github.com/login/oauth/access_token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                String error = tokenResponse != null ? (String) tokenResponse.get("error_description") : "unknown";
                throw new OAuthException(HttpStatus.BAD_REQUEST, "OAUTH_TOKEN_INVALID", "Không thể trao đổi mã code GitHub: " + error);
            }

            String accessToken = (String) tokenResponse.get("access_token");

            // Fetch GitHub Profile
            Map<String, Object> profileResponse = restClient.get()
                    .uri("https://api.github.com/user")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (profileResponse == null || !profileResponse.containsKey("id")) {
                throw new OAuthException(HttpStatus.BAD_REQUEST, "OAUTH_TOKEN_INVALID", "Không thể lấy thông tin profile GitHub.");
            }

            String email = (String) profileResponse.get("email");
            
            // If email is private/null, retrieve emails list
            if (email == null || email.trim().isEmpty()) {
                try {
                    List<Map<String, Object>> emails = restClient.get()
                            .uri("https://api.github.com/user/emails")
                            .header("Authorization", "Bearer " + accessToken)
                            .retrieve()
                            .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

                    if (emails != null) {
                        for (Map<String, Object> emailObj : emails) {
                            Boolean primary = (Boolean) emailObj.get("primary");
                            Boolean verified = (Boolean) emailObj.get("verified");
                            if (primary != null && primary && verified != null && verified) {
                                email = (String) emailObj.get("email");
                                break;
                            }
                        }
                        if (email == null && !emails.isEmpty()) {
                            email = (String) emails.get(0).get("email");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Failed to fetch private GitHub emails: " + e.getMessage());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("id", String.valueOf(profileResponse.get("id")));
            result.put("email", email);
            result.put("name", profileResponse.get("name"));
            result.put("accessToken", accessToken);
            result.put("refreshToken", tokenResponse.get("refresh_token"));
            return result;
        } catch (Exception e) {
            if (e instanceof OAuthException) throw (OAuthException) e;
            throw new OAuthException(HttpStatus.BAD_REQUEST, "OAUTH_TOKEN_INVALID", "Không thể xác thực code GitHub: " + e.getMessage());
        }
    }

    /**
     * Handles login, auto-linking, or auto-registration.
     */
    public Map<String, Object> loginOrRegisterOAuth(String provider, String providerUid, String email, 
                                                    String name, String password, String accessToken, String refreshToken) {
        if (email == null || email.trim().isEmpty()) {
            throw new OAuthException(HttpStatus.BAD_REQUEST, "OAUTH_EMAIL_NOT_VERIFIED", "Tài khoản mạng xã hội không trả về email hoặc email chưa được xác minh.");
        }

        // 1. Look up existing link
        Optional<OAuthAccount> linkedAccountOpt = oauthAccountRepository.findByProviderAndProviderUid(provider, providerUid);
        if (linkedAccountOpt.isPresent()) {
            OAuthAccount linkedAccount = linkedAccountOpt.get();
            // Update token info
            linkedAccount.setAccessToken(accessToken);
            if (refreshToken != null) {
                linkedAccount.setRefreshToken(refreshToken);
            }
            oauthAccountRepository.save(linkedAccount);
            return generateAuthResponse(linkedAccount.getUser());
        }

        // 2. No link, look up user by email
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Password confirmation required to link existing account
            if (password == null || password.trim().isEmpty()) {
                throw new OAuthException(HttpStatus.BAD_REQUEST, "OAUTH_PASSWORD_CONFIRM_REQUIRED", "Vui lòng nhập mật khẩu tài khoản hiện tại để liên kết tự động.");
            }

            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                throw new OAuthException(HttpStatus.BAD_REQUEST, "OAUTH_PASSWORD_CONFIRM_REQUIRED", "Mật khẩu xác nhận không chính xác.");
            }

            // Link account
            OAuthAccount newLink = OAuthAccount.builder()
                    .user(user)
                    .provider(provider)
                    .providerUid(providerUid)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
            oauthAccountRepository.save(newLink);

            return generateAuthResponse(user);
        }

        // 3. User does not exist, auto-register
        User.UserType userType = email.endsWith("@fpt.edu.vn") ? User.UserType.INTERNAL : User.UserType.EXTERNAL;
        User.Status status = (userType == User.UserType.INTERNAL) ? User.Status.APPROVED : User.Status.PENDING;

        User user = User.builder()
                .fullName(name != null && !name.trim().isEmpty() ? name : email.split("@")[0])
                .email(email)
                .passwordHash(null) // No local password initially
                .role(User.Role.STUDENT)
                .userType(userType)
                .status(status)
                .isTempAccount(false)
                .isDeptHead(false)
                .emailVerifiedAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        OAuthAccount newLink = OAuthAccount.builder()
                .user(savedUser)
                .provider(provider)
                .providerUid(providerUid)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
        oauthAccountRepository.save(newLink);

        return generateAuthResponse(savedUser);
    }

    /**
     * Link current logged-in user to Google.
     */
    public void linkGoogle(User user, String idToken) {
        Map<String, Object> googleInfo = verifyGoogleToken(idToken);
        String sub = (String) googleInfo.get("sub");
        String email = (String) googleInfo.get("email");

        if (!user.getEmail().equalsIgnoreCase(email)) {
            throw new OAuthException(HttpStatus.BAD_REQUEST, "OAUTH_EMAIL_MISMATCH", "Email của Google không trùng với email tài khoản hiện tại.");
        }

        Optional<OAuthAccount> existingLink = oauthAccountRepository.findByProviderAndProviderUid("google", sub);
        if (existingLink.isPresent()) {
            if (existingLink.get().getUser().getId().equals(user.getId())) {
                return; // already linked to this user
            }
            throw new OAuthException(HttpStatus.BAD_REQUEST, "OAUTH_ACCOUNT_ALREADY_LINKED", "Tài khoản Google này đã được liên kết với một tài khoản khác.");
        }

        OAuthAccount newLink = OAuthAccount.builder()
                .user(user)
                .provider("google")
                .providerUid(sub)
                .build();
        oauthAccountRepository.save(newLink);
    }

    /**
     * Link current logged-in user to GitHub.
     */
    public void linkGithub(User user, String code, String redirectUri) {
        Map<String, Object> githubInfo = verifyGithubCode(code, redirectUri);
        String id = (String) githubInfo.get("id");
        String email = (String) githubInfo.get("email");
        String accessToken = (String) githubInfo.get("accessToken");
        String refreshToken = (String) githubInfo.get("refreshToken");

        if (email != null && !user.getEmail().equalsIgnoreCase(email)) {
            throw new OAuthException(HttpStatus.BAD_REQUEST, "OAUTH_EMAIL_MISMATCH", "Email của GitHub không trùng với email tài khoản hiện tại.");
        }

        Optional<OAuthAccount> existingLink = oauthAccountRepository.findByProviderAndProviderUid("github", id);
        if (existingLink.isPresent()) {
            if (existingLink.get().getUser().getId().equals(user.getId())) {
                return; // already linked to this user
            }
            throw new OAuthException(HttpStatus.BAD_REQUEST, "OAUTH_ACCOUNT_ALREADY_LINKED", "Tài khoản GitHub này đã được liên kết với một tài khoản khác.");
        }

        OAuthAccount newLink = OAuthAccount.builder()
                .user(user)
                .provider("github")
                .providerUid(id)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
        oauthAccountRepository.save(newLink);
    }

    /**
     * Unlink Google account.
     */
    public void unlinkGoogle(User user) {
        OAuthAccount link = oauthAccountRepository.findByUserAndProvider(user, "google")
                .orElseThrow(() -> new ResourceNotFoundException("Liên kết Google không tồn tại."));

        validateUnlinkAllowed(user, "google");
        oauthAccountRepository.delete(link);
    }

    /**
     * Unlink GitHub account.
     */
    public void unlinkGithub(User user) {
        OAuthAccount link = oauthAccountRepository.findByUserAndProvider(user, "github")
                .orElseThrow(() -> new ResourceNotFoundException("Liên kết GitHub không tồn tại."));

        validateUnlinkAllowed(user, "github");
        oauthAccountRepository.delete(link);
    }

    private void validateUnlinkAllowed(User user, String currentProvider) {
        boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().trim().isEmpty();
        
        // Check if there are other oauth accounts linked
        List<OAuthAccount> links = oauthAccountRepository.findByUser(user);
        boolean hasOtherOauth = links.stream().anyMatch(l -> !l.getProvider().equals(currentProvider));

        if (!hasPassword && !hasOtherOauth) {
            throw new OAuthException(HttpStatus.BAD_REQUEST, "OAUTH_UNLINK_FORBIDDEN", 
                    "Không thể gỡ liên kết social duy nhất khi tài khoản của bạn chưa được thiết lập mật khẩu.");
        }
    }

    /**
     * Utility to generate JWT token response.
     */
    public Map<String, Object> generateAuthResponse(User user) {
        if (user.getStatus() == User.Status.REJECTED) {
            throw new OAuthException(HttpStatus.FORBIDDEN, "REJECTED_NOT_ALLOWED_LOGIN", "Tài khoản đã bị từ chối.");
        }

        String token = jwtTokenUtil.generateToken(user.getEmail(), user.getRole().name());
        Map<String, Object> resp = new HashMap<>();
        resp.put("accessToken", token);
        resp.put("refreshToken", token);
        resp.put("email", user.getEmail());
        resp.put("status", user.getStatus().name());
        resp.put("role", user.getRole().name());
        resp.put("userId", user.getId());

        // Include nested user object for maximum compatibility
        Map<String, Object> userObj = new HashMap<>();
        userObj.put("id", user.getId());
        userObj.put("userId", user.getId());
        userObj.put("email", user.getEmail());
        userObj.put("status", user.getStatus().name());
        userObj.put("role", user.getRole().name());
        resp.put("user", userObj);

        return resp;
    }
}
