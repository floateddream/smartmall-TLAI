package com.smartmall.tlai.security.auth;

import com.smartmall.tlai.business.user.application.UserAccountService;
import com.smartmall.tlai.business.user.domain.BuiltInRoleCodes;
import com.smartmall.tlai.business.user.domain.UserAccount;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import com.smartmall.tlai.security.AppUserPrincipal;
import com.smartmall.tlai.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

/** 注册和登录用例，不依赖 HTTP 请求 DTO。 */
@Service
public class AuthService {
    private final UserAccountService userAccountService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserAccountService userAccountService,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userAccountService = userAccountService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthenticatedUser register(RegisterCommand command) {
        UserAccount user = userAccountService.createUser(command.username(), command.email(),
                command.password(), BuiltInRoleCodes.OPERATOR);
        return AuthenticatedUser.from(user);
    }

    public AuthResult login(LoginCommand command) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(command.account().trim(), command.password()));
        } catch (AuthenticationException exception) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return new AuthResult("Bearer", jwtService.createAccessToken(principal),
                jwtService.accessTokenTtlSeconds(), AuthenticatedUser.from(principal));
    }

    public AuthenticatedUser currentUser(AppUserPrincipal principal) {
        return AuthenticatedUser.from(principal);
    }
}
