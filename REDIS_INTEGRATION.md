# Hướng Dẫn Tích Hợp Redis Caching (Song Song Cùng MySQL)

Tài liệu này hướng dẫn chi tiết cách cấu hình **Redis** làm bộ nhớ đệm (Caching Layer) hoạt động song song cùng **MySQL** (Database chính) trong dự án Hackathon Management.

---

## 🏗️ 1. Cập Nhật Thư Viện (`be/pom.xml`)

Thay thế thư viện cache cũ (`caffeine`) bằng thư viện Redis của Spring Boot. Mở file [pom.xml](file:///d:/Code/Spring/HackathonManagement/be/pom.xml) và cập nhật:

**Xóa thư viện Caffeine:**
```xml
		<!-- Thư viện Caffeine Cache in-memory -->
		<dependency>
			<groupId>com.github.ben-manes.caffeine</groupId>
			<artifactId>caffeine</artifactId>
		</dependency>
```

**Thêm thư viện Redis:**
```xml
		<!-- Thư viện Spring Boot Starter Data Redis -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-redis</artifactId>
		</dependency>
```

---

## ⚙️ 2. Cấu Hinh Tham Số Kết Nối (`application.properties`)

Cấu hình cho Spring Boot biết sẽ sử dụng Redis ở cổng `6379` local và MySQL làm DB chính.

Mở file [application.properties](file:///d:/Code/Spring/HackathonManagement/be/src/main/resources/application.properties) và thêm các dòng cấu hình sau:

```properties
# ============================================================
# REDIS CACHING CONFIGURATION (Local Default)
# ============================================================
spring.cache.type=redis

# Lưu ý: Từ Spring Boot 3.x, nên sử dụng tiền tố spring.data.redis thay vì spring.redis
spring.data.redis.host=127.0.0.1
spring.data.redis.port=6379
spring.data.redis.password=

# Cấu hình Connection Pool cho Lettuce (Thư viện kết nối Redis mặc định của Spring Boot)
spring.data.redis.lettuce.pool.max-active=7
spring.data.redis.lettuce.pool.max-idle=7
spring.data.redis.lettuce.pool.min-idle=2
```

> [!NOTE]
> Bạn vẫn giữ nguyên cấu hình MySQL (`spring.datasource.url=jdbc:mysql://localhost:3306/sealhackathon`) như cũ. MySQL giữ vai trò lưu trữ vĩnh viễn, còn Redis chỉ đóng vai trò lưu tạm (cache) dữ liệu để tăng tốc độ phản hồi API.

---

## 📝 3. Cập Nhật Code Cấu Hình Cache (`CacheConfig.java`)

Bạn cần chuyển cấu hình của `CacheConfig` từ `CaffeineCacheManager` sang `RedisCacheManager`. Chúng ta sẽ sử dụng **JSON Serializer** để dữ liệu lưu trên Redis có định dạng JSON dễ đọc/debug và tránh lỗi ClassCastException khi cập nhật mã nguồn Java.

Hãy cập nhật toàn bộ nội dung file [CacheConfig.java](file:///d:/Code/Spring/HackathonManagement/be/src/main/java/hackthon/mangaement/hackathon/config/CacheConfig.java) bằng code mới sau:

```java
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
```

---

## ⚡ 4. Cách Sử Dụng Caching Trong Code

Spring Boot hỗ trợ các Annotation rất tiện lợi, bạn chỉ cần gắn chúng trên các phương thức của tầng Controller hoặc Service:

*   **`@Cacheable(value = "rounds", key = "#id")`**: Đọc dữ liệu từ cache có tên `rounds` với khóa là `id`. Nếu không có trong cache, hàm sẽ chạy bình thường rồi lưu kết quả vào cache.
*   **`@CacheEvict(value = "rounds", key = "#roundId")`**: Xóa bản ghi trong cache có tên `rounds` với khóa là `roundId` (thường dùng khi thực hiện cập nhật hoặc xóa dữ liệu của Round đó để đảm bảo dữ liệu cache không bị cũ).

---

## 🐳 5. Khởi Động Redis Nhanh Bằng Docker (Để Test Local)

Nếu máy của bạn chưa có Redis, cách nhanh nhất là chạy bằng Docker:

```bash
docker run -d --name redis-local -p 6379:6379 redis:7-alpine
```
