package com.forgeauth.infrastructure.security;

import com.forgeauth.domain.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final RsaKeyManager rsaKeyManager;

    @Value("${forgeauth.jwt.access-token-expiration-sec:900}")
    private long accessTokenExpirationSec;

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenExpirationSec);

        return Jwts.builder()
                .issuer("https://forgeauth.dev")
                .subject(user.getId().toString())
                .audience().add("forgeauth-clients").and()
                .expiration(Date.from(expiry))
                .issuedAt(Date.from(now))
                .id(UUID.randomUUID().toString())
                .claim("email", user.getEmail())
                .claim("email_verified", user.isEmailVerified())
                .claim("name", user.getDisplayName())
                .claim("status", user.getStatus().name())
                .signWith(rsaKeyManager.getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        // Refresh token can just be a secure random UUID string.
        // It does not need to be a JWT, because we look it up from the database by hash.
        return UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
    }

    public long getAccessTokenExpirationSec() {
        return accessTokenExpirationSec;
    }

    public Claims validateAndParseToken(String token) {
        return Jwts.parser()
                .verifyWith(rsaKeyManager.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
