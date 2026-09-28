package com.smartmall.tlai.security;

import com.smartmall.tlai.business.user.domain.UserAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Spring Security 使用的登录用户快照。 */
public record AppUserPrincipal(Long id, String username, String email, String password,
                               List<String> roleCodes, List<String> permissions,
                               boolean enabled) implements UserDetails {
    public AppUserPrincipal {
        roleCodes = roleCodes == null ? List.of() : List.copyOf(roleCodes);
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }

    public static AppUserPrincipal from(UserAccount user) {
        return new AppUserPrincipal(user.id(), user.username(), user.email(), user.passwordHash(),
                user.roleCodes(), user.permissions(), user.enabled());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>(roleCodes.size() + permissions.size());
        roleCodes.stream().map(code -> new SimpleGrantedAuthority("ROLE_" + code)).forEach(authorities::add);
        permissions.stream().map(SimpleGrantedAuthority::new).forEach(authorities::add);
        return List.copyOf(authorities);
    }

    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return password; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }
}
