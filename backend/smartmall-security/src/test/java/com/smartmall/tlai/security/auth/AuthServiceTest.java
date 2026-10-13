package com.smartmall.tlai.security.auth;

import com.smartmall.tlai.business.access.application.RegistrationPolicyService;
import com.smartmall.tlai.business.user.application.UserAccountService;
import com.smartmall.tlai.business.user.domain.BuiltInRoleCodes;
import com.smartmall.tlai.business.user.domain.UserAccount;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import com.smartmall.tlai.security.AppUserPrincipal;
import com.smartmall.tlai.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserAccountService userAccountService;
    @Mock RegistrationPolicyService registrationPolicyService;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userAccountService, registrationPolicyService,
                authenticationManager, jwtService);
    }

    @Test
    void shouldRegisterOperator() {
        RegisterCommand command = new RegisterCommand("test_user", "Test@Example.com", "password1");
        UserAccount user = user(1L, "test_user", "test@example.com", BuiltInRoleCodes.OPERATOR);
        when(userAccountService.createUser(command.username(), command.email(), command.password(), BuiltInRoleCodes.OPERATOR))
                .thenReturn(user);

        AuthenticatedUser result = authService.register(command);

        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.roles()).containsExactly(BuiltInRoleCodes.OPERATOR);
        verify(registrationPolicyService).requireRegistrationEnabled();
        verify(userAccountService).createUser(command.username(), command.email(), command.password(), BuiltInRoleCodes.OPERATOR);
    }

    @Test
    void shouldRejectRegistrationWhenPublicRegistrationIsClosed() {
        RegisterCommand command = new RegisterCommand("test_user", "test@example.com", "password1");
        doThrow(new BusinessException(ErrorCode.REGISTRATION_CLOSED))
                .when(registrationPolicyService).requireRegistrationEnabled();

        assertThatThrownBy(() -> authService.register(command))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.REGISTRATION_CLOSED));
        verifyNoInteractions(userAccountService);
    }

    @Test
    void shouldLoginWithUsernameOrEmail() {
        LoginCommand command = new LoginCommand("test@example.com", "password1");
        AppUserPrincipal principal = new AppUserPrincipal(1L, "test_user", "test@example.com",
                "encoded-password", List.of(BuiltInRoleCodes.OPERATOR), List.of("order:view"), true);
        var authenticated = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticated);
        when(jwtService.createAccessToken(principal)).thenReturn("access-token");
        when(jwtService.accessTokenTtlSeconds()).thenReturn(7200L);

        AuthResult result = authService.login(command);

        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.user().username()).isEqualTo("test_user");
    }

    @Test
    void shouldReturnInvalidCredentialsWhenAuthenticationFails() {
        LoginCommand command = new LoginCommand("test_user", "wrong-password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(command))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void shouldHideDisabledAccountBehindGenericCredentialError() {
        LoginCommand command = new LoginCommand("disabled_user", "password1");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User is disabled"));

        assertThatThrownBy(() -> authService.login(command))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldPropagateUnexpectedAuthenticationInfrastructureFailure() {
        LoginCommand command = new LoginCommand("test_user", "password1");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new IllegalStateException("identity store unavailable"));

        assertThatThrownBy(() -> authService.login(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("identity store unavailable");
        verifyNoInteractions(jwtService);
    }

    private UserAccount user(Long id, String username, String email, String role) {
        OffsetDateTime now = OffsetDateTime.parse("2030-01-01T00:00:00Z");
        return new UserAccount(id, username, email, "encoded-password", true,
                List.of(role), List.of("order:view"), now, now);
    }
}
