package com.smartmall.tlai.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    @Autowired
    public JwtService(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(AppUserPrincipal principal) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        return Jwts.builder().subject(principal.username())
                .claim("uid", principal.id()).claim("roles", principal.roleCodes())
                .issuedAt(Date.from(issuedAt)).expiration(Date.from(expiresAt))
                .signWith(signingKey).compact();
    }

    public String extractUsername(String token) { return parseClaims(token).getSubject(); }

    public boolean isValid(String token, AppUserPrincipal principal) {
        Claims claims = parseClaims(token);
        return principal.enabled() && principal.username().equals(claims.getSubject())
                && claims.getExpiration().toInstant().isAfter(clock.instant());
    }

    public long accessTokenTtlSeconds() { return properties.accessTokenTtl().toSeconds(); }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }
}
