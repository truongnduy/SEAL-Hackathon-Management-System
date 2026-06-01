package hackthon.mangaement.hackathon.model.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "oauth_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String provider; // 'google' | 'github'

    @Column(name = "provider_uid", nullable = false)
    private String providerUid;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "expires_at")
    private java.time.LocalDateTime expiresAt;
}
