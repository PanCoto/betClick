package com.betclick.service;

import com.betclick.dto.request.LoginRequest;
import com.betclick.dto.request.RegisterRequest;
import com.betclick.dto.response.LoginResponse;
import com.betclick.model.User;
import com.betclick.model.enums.UserRole;
import com.betclick.repository.UserRepository;
import com.betclick.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private BonusService bonusService;
    private UserLevelService userLevelService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByLogin(request.getLogin())) {
            throw new IllegalArgumentException("Uzytkownik o loginie " + request.getLogin() + " juz istnieje!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Adres email " + request.getEmail() + " jest juz zajety!");
        }

        User user = User.builder()
                .login(request.getLogin())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .firstName(defaultText(request.getFirstName(), request.getLogin()))
                .lastName(defaultText(request.getLastName(), ""))
                .dateOfBirth(request.getDateOfBirth() != null ? request.getDateOfBirth() : LocalDate.of(2000, 1, 1))
                .phoneNumber(request.getPhoneNumber())
                .balance(BigDecimal.ZERO)
                .isActive(true)
                .role(UserRole.PLAYER)
                .registrationDate(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        if (userLevelService != null) {
            userLevelService.updateLevel(savedUser);
        }
        if (bonusService != null) {
            bonusService.grantWelcomeBonus(savedUser);
        }
        return savedUser;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword())
        );

        String token = jwtTokenProvider.generateToken(auth.getName());
        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_PLAYER")
                .replace("ROLE_", "");

        return LoginResponse.builder()
                .token(token)
                .login(auth.getName())
                .role(role)
                .build();
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    @Autowired(required = false)
    public void setBonusService(BonusService bonusService) {
        this.bonusService = bonusService;
    }

    @Autowired(required = false)
    public void setUserLevelService(UserLevelService userLevelService) {
        this.userLevelService = userLevelService;
    }
}
