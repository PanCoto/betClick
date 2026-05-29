package com.betclick.security;

import com.betclick.model.User;
import com.betclick.model.enums.UserRole;
import com.betclick.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByLogin(username)
                .orElseThrow(() -> new UsernameNotFoundException("Nie znaleziono uzytkownika: " + username));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new DisabledException("Konto uzytkownika jest zablokowane");
        }
        if (!StringUtils.hasText(user.getPasswordHash())) {
            throw new UsernameNotFoundException("Konto uzytkownika nie ma ustawionego hasla: " + username);
        }

        UserRole role = user.getRole() != null ? user.getRole() : UserRole.PLAYER;

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getLogin())
                .password(user.getPasswordHash())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name())))
                .disabled(!Boolean.TRUE.equals(user.getIsActive()))
                .build();
    }

    private static class DisabledException extends UsernameNotFoundException {
        public DisabledException(String msg) {
            super(msg);
        }
    }
}
