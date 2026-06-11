package com.forgeauth.server.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgeauth.common.dto.ErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @org.springframework.beans.factory.annotation.Value("${forgeauth.rate-limiting.enabled:true}")
    private boolean enabled;

    // Simple in-memory bucket store for initial scaffolding.
    // In production, use Redis-backed Bucket4j for distributed rate limiting.
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        
        // Only rate limit specific auth endpoints for now
        if (enabled && path.startsWith("/api/v1/auth")) {
            String ip = request.getRemoteAddr();
            Bucket bucket = resolveBucket(ip);

            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                sendRateLimitError(response);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private Bucket resolveBucket(String ip) {
        return cache.computeIfAbsent(ip, this::newBucket);
    }

    private Bucket newBucket(String ip) {
        // 10 requests per minute per IP for Auth endpoints
        Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private void sendRateLimitError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");

        ErrorResponse error = ErrorResponse.builder()
                .error(ErrorResponse.ErrorDetails.builder()
                        .code("AUTH_RATE_LIMITED")
                        .message("Too many requests. Please try again later.")
                        .status(HttpStatus.TOO_MANY_REQUESTS.value())
                        .timestamp(Instant.now())
                        .traceId(UUID.randomUUID().toString())
                        .build())
                .build();

        objectMapper.writeValue(response.getWriter(), error);
    }
}
