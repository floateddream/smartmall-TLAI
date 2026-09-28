package com.smartmall.tlai.security.auth;

import com.smartmall.tlai.business.user.domain.UserAccount;
import com.smartmall.tlai.security.AppUserPrincipal;

import java.util.List;

public record AuthenticatedUser(Long id, String username, String email,
                                List<String> roles, List<String> permissions, boolean enabled) {
    public static AuthenticatedUser from(UserAccount user) {
        return new AuthenticatedUser(user.id(), user.username(), user.email(),
                user.roleCodes(), user.permissions(), user.enabled());
    }

    public static AuthenticatedUser from(AppUserPrincipal principal) {
        return new AuthenticatedUser(principal.id(), principal.username(), principal.email(),
                principal.roleCodes(), principal.permissions(), principal.enabled());
    }
}
