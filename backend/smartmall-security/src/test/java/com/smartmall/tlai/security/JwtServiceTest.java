package com.smartmall.tlai.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
}
