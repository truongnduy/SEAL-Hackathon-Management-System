package hackthon.mangaement.hackathon.repository;

import hackthon.mangaement.hackathon.model.User.OAuthAccount;
import hackthon.mangaement.hackathon.model.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Integer> {
    Optional<OAuthAccount> findByProviderAndProviderUid(String provider, String providerUid);
    Optional<OAuthAccount> findByUserAndProvider(User user, String provider);
    List<OAuthAccount> findByUser(User user);
}
