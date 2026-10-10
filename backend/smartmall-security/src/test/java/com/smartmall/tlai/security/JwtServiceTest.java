package com.smartmall.tlai.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private static final String SECRET = "smartmall-test-secret-that-is-longer-than-thirty-two-characters";

    @Test
    void shouldCreateAndValidateAccessToken() {
        Clock clock = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);
        JwtService service = new JwtService(new JwtProperties(SECRET, Duration.ofHours(2)), clock);
        AppUserPrincipal principal = new AppUserPrincipal(1L, "test_user", "test@example.com",
                "encoded-password", List.of("OPERATOR"), List.of("order:view"), true);

        String token = service.createAccessToken(principal);

        assertThat(service.extractUsername(token)).isEqualTo("test_user");
        assertThat(service.isValid(token, principal)).isTrue();
        assertThat(service.accessTokenTtlSeconds()).isEqualTo(7200);
        assertThat(principal.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_OPERATOR", "order:view");
    }

    @Test
    void shouldRejectTokenForDisabledUser() {
        Clock clock = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);
        JwtService service = new JwtService(new JwtProperties(SECRET, Duration.ofHours(2)), clock);
        AppUserPrincipal enabled = new AppUserPrincipal(1L, "test_user", "test@example.com",
                "encoded-password", List.of("OPERATOR"), List.of("order:view"), true);
        AppUserPrincipal disabled = new AppUserPrincipal(1L, "test_user", "test@example.com",
                "encoded-password", List.of("OPERATOR"), List.of("order:view"), false);

        String token = service.createAccessToken(enabled);

        assertThat(service.isValid(token, disabled)).isFalse();
    }

    @Test
    void shouldRejectExpiredToken() {
        Instant issuedAt = Instant.parse("2020-01-01T00:00:00Z");
        JwtProperties properties = new JwtProperties(SECRET, Duration.ofMinutes(5));
        JwtService issuer = new JwtService(properties, Clock.fixed(issuedAt, ZoneOffset.UTC));
        AppUserPrincipal principal = principal("test_user");
        String token = issuer.createAccessToken(principal);
        JwtService verifier = new JwtService(properties,
                Clock.fixed(issuedAt.plus(Duration.ofMinutes(6)), ZoneOffset.UTC));

        assertThatThrownBy(() -> verifier.isValid(token, principal))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void shouldRejectTamperedToken() {
        Clock clock = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);
        JwtService service = new JwtService(new JwtProperties(SECRET, Duration.ofHours(2)), clock);
        String token = service.createAccessToken(principal("test_user"));
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'a' ? 'b' : 'a';
        String tampered = token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);

        assertThatThrownBy(() -> service.extractUsername(tampered))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    void shouldRejectTokenBelongingToAnotherUser() {
        Clock clock = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);
        JwtService service = new JwtService(new JwtProperties(SECRET, Duration.ofHours(2)), clock);

        assertThat(service.isValid(service.createAccessToken(principal("alice")), principal("bob"))).isFalse();
    }

    private AppUserPrincipal principal(String username) {
        return new AppUserPrincipal(1L, username, username + "@example.com",
                "encoded-password", List.of("OPERATOR"), List.of("order:view"), true);
    }
}
