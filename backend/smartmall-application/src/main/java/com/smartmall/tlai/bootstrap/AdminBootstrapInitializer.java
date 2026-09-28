package com.smartmall.tlai.bootstrap;

import com.smartmall.tlai.business.user.application.UserAccountService;
import com.smartmall.tlai.business.user.domain.BuiltInRoleCodes;
import com.smartmall.tlai.business.user.domain.UserValidationRules;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AdminBootstrapInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapInitializer.class);
    private final BootstrapAdminProperties properties;
    private final UserAccountService userAccountService;
    private final Validator validator;

    public AdminBootstrapInitializer(BootstrapAdminProperties properties,
                                     UserAccountService userAccountService,
                                     Validator validator) {
        this.properties = properties;
        this.userAccountService = userAccountService;
        this.validator = validator;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) return;
        if (userAccountService.hasAdmin()) {
            log.info("Bootstrap administrator already exists; initialization skipped");
            return;
        }
        BootstrapAdminInput input = new BootstrapAdminInput(
                properties.username(), properties.email(), properties.password());
        Set<ConstraintViolation<BootstrapAdminInput>> violations = validator.validate(input);
        if (!violations.isEmpty()) {
            String message = violations.stream().map(ConstraintViolation::getMessage)
                    .sorted().collect(Collectors.joining("; "));
            throw new IllegalStateException("Invalid bootstrap administrator configuration: " + message);
        }
        userAccountService.createUser(input.username(), input.email(), input.password(), BuiltInRoleCodes.ADMIN);
        log.info("Bootstrap administrator created: {}", input.username());
    }

    private record BootstrapAdminInput(
            @NotBlank @Pattern(regexp = UserValidationRules.USERNAME_PATTERN) String username,
            @NotBlank @Email @Size(max = 128) String email,
            @NotBlank @Pattern(regexp = UserValidationRules.PASSWORD_PATTERN) String password) {
    }
}
