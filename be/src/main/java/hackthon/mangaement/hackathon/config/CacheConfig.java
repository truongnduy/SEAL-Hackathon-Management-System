package hackthon.mangaement.hackathon.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        // 1. Vùng Cache cho Hackathon: Lưu tối đa 100 bản ghi, hết hạn sau 10 phút
        CaffeineCache hackathonsCache = new CaffeineCache("hackathons", 
            Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(100)
                .build());

        // 2. Vùng Cache cho Vòng thi (Rounds): Lưu tối đa 200 bản ghi, hết hạn sau 5 phút
        CaffeineCache roundsCache = new CaffeineCache("rounds", 
            Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(200)
                .build());

        // 3. Vùng Cache cho Bảng đấu (Tracks): Hết hạn sau 5 phút
        CaffeineCache tracksCache = new CaffeineCache("tracks", 
            Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .build());

        cacheManager.setCaches(Arrays.asList(hackathonsCache, roundsCache, tracksCache));
        return cacheManager;
    }
}
