package com.fleettrack.auth.service;

import com.fleettrack.auth.entity.*;
import com.fleettrack.auth.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
        name = "app.bootstrap-admin.enabled",
        havingValue = "true"
)
public class BootstrapAdminInitializer
        implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final String username;
    private final String email;
    private final String password;

    public BootstrapAdminInitializer(
            AppUserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.username}")
            String username,
            @Value("${app.bootstrap-admin.email}")
            String email,
            @Value("${app.bootstrap-admin.password}")
            String password
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {

        if (userRepository.existsByUsername(username)) {
            return;
        }

        Role adminRole = roleRepository
                .findByName(RoleName.ADMIN)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "ADMIN role not found"
                        )
                );

        AppUser admin = new AppUser();

        admin.setUsername(username);
        admin.setEmail(email);
        admin.setPasswordHash(
                passwordEncoder.encode(password)
        );
        admin.setEnabled(true);

        admin.getRoles().add(adminRole);

        userRepository.save(admin);
    }
}