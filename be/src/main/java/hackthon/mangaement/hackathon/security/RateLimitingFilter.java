package hackthon.mangaement.hackathon.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    @Value("${rate.limit.enabled:true}")
    private boolean enabled;

    @Value("${rate.limit.capacity:60}")
    private long capacity;

    @Value("${rate.limit.refill-tokens:60}")
    private long refillTokens;

    @Value("${rate.limit.refill-period-seconds:60}")
    private long refillPeriodSeconds;

    private final ConcurrentHashMap<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(request);
        Bucket bucket = cache.computeIfAbsent(ip, k -> new Bucket(capacity, refillTokens, refillPeriodSeconds));

        if (!bucket.tryConsume()) {
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\": \"Too Many Requests\", \"message\": \"You have exceeded the rate limit. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isEmpty()) {
            return xri;
        }
        return request.getRemoteAddr();
    }

    private static class Bucket {
        private final long capacity;
        private final double refillRate;
        private double tokens;
        private long lastRefillTimestamp;

        public Bucket(long capacity, long refillTokens, long refillPeriodSeconds) {
            this.capacity = capacity;
            this.refillRate = (double) refillTokens / (refillPeriodSeconds * 1000.0);
            this.tokens = capacity;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsedTime = now - lastRefillTimestamp;
            if (elapsedTime > 0) {
                double refillAmount = elapsedTime * refillRate;
                tokens = Math.min(capacity, tokens + refillAmount);
                lastRefillTimestamp = now;
            }
        }
    }
}
