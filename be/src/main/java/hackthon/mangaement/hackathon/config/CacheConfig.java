package hackthon.mangaement.hackathon.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Cấu hình chung mặc định cho Redis cache
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // Thời gian sống mặc định (TTL) là 10 phút
                .disableCachingNullValues()       // Không lưu giá trị null vào cache
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // Cấu hình thời gian sống (TTL) chi tiết cho từng vùng cache cụ thể
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Vùng cache hackathons: lưu 10 phút
        cacheConfigurations.put("hackathons", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // Vùng cache rounds: lưu 5 phút
        cacheConfigurations.put("rounds", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Vùng cache tracks: lưu 5 phút
        cacheConfigurations.put("tracks", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Vùng cache criteria: lưu 10 phút (ít thay đổi)
        cacheConfigurations.put("criteria", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Vùng cache leaderboard: lưu 10 phút (giảm tải truy vấn nặng trên view)
        cacheConfigurations.put("leaderboard", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}