package com.smartmall.tlai.security;

import com.smartmall.tlai.business.user.application.UserAccountService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Spring Security 与业务用户模块之间的适配层。 */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserAccountService userAccountService;

    public DatabaseUserDetailsService(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @Override
    public UserDetails loadUserByUsername(String account) throws UsernameNotFoundException {
        return userAccountService.findByLogin(account).map(AppUserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
