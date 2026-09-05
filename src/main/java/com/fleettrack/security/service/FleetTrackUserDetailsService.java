package com.fleettrack.security.service;

import com.fleettrack.auth.entity.AppUser;
import com.fleettrack.auth.repository.AppUserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class FleetTrackUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    public FleetTrackUserDetailsService(
            AppUserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found")
                );

        String[] authorities = user.getRoles()
                .stream()
                .map(role -> "ROLE_" + role.getName().name())
                .toArray(String[]::new);

        return User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .disabled(!user.isEnabled())
                .build();
    }
}