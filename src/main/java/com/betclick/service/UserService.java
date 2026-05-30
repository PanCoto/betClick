package com.betclick.service;

import com.betclick.dto.response.UserProfileResponse;
import com.betclick.model.User;
import com.betclick.model.enums.UserRole;
import com.betclick.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final int DEFAULT_USER_LIMIT = 20;

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private DictionaryService dictionaryService;
    private NotificationService notificationService;

    public UserService(UserRepository userRepository, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String login) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Nie znaleziono użytkownika: " + login));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new UsernameNotFoundException("Konto uzytkownika jest nieaktywne: " + login);
        }

        UserRole role = user.getRole() != null ? user.getRole() : UserRole.PLAYER;
        BigDecimal balance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        String firstName = StringUtils.hasText(user.getFirstName()) ? user.getFirstName() : user.getLogin();
        String lastName = StringUtils.hasText(user.getLastName()) ? user.getLastName() : "";

        UserProfileResponse.UserProfileResponseBuilder builder = UserProfileResponse.builder()
                .login(user.getLogin())
                .email(user.getEmail())
                .firstName(firstName)
                .lastName(lastName)
                .dateOfBirth(user.getDateOfBirth())
                .phoneNumber(user.getPhoneNumber())
                .balance(balance)
                .registrationDate(user.getRegistrationDate())
                .role(role.name())
                .levelCode(dictionaryService != null ? dictionaryService.userLevelCode(user.getLevelId()) : "BRONZE")
                .levelName(dictionaryService != null ? dictionaryService.userLevelName(user.getLevelId()) : "Bronze")
                .availableBonusAmount(dictionaryService != null ? dictionaryService.availableBonusAmount(user.getId()) : BigDecimal.ZERO)
                .unreadNotificationsCount(notificationService != null ? notificationService.countUnread(user.getId()) : 0L);

        String sql = "SELECT total_bets, won_bets, lost_bets, active_bets, total_staked, total_won, win_rate FROM get_user_stats(?)";
        try {
            Map<String, Object> stats = jdbcTemplate.queryForMap(sql, user.getId());
            builder.totalBets(((Number) stats.get("total_bets")).longValue())
                    .wonBets(((Number) stats.get("won_bets")).longValue())
                    .lostBets(((Number) stats.get("lost_bets")).longValue())
                    .activeBets(((Number) stats.get("active_bets")).longValue())
                    .totalStaked((BigDecimal) stats.get("total_staked"))
                    .totalWon((BigDecimal) stats.get("total_won"))
                    .winRate((BigDecimal) stats.get("win_rate"));
        } catch (Exception e) {
            log.warn("Could not load user stats for user id {}", user.getId(), e);
            builder.totalBets(0L)
                    .wonBets(0L)
                    .lostBets(0L)
                    .activeBets(0L)
                    .totalStaked(BigDecimal.ZERO)
                    .totalWon(BigDecimal.ZERO)
                    .winRate(BigDecimal.ZERO);
        }

        return builder.build();
    }

    @Transactional
    public void toggleUserActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Użytkownik o ID " + userId + " nie istnieje!"));
        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        userRepository.save(user);
    }

    @Transactional
    public void changeUserRole(Long userId, UserRole role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Użytkownik o ID " + userId + " nie istnieje!"));
        user.setRole(role);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return findUsers(DEFAULT_USER_LIMIT);
    }

    @Transactional(readOnly = true)
    public Page<User> findUsersPage(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<User> findUsers(int limit) {
        return userRepository.findAll(PageRequest.of(0, safeSize(limit), Sort.by("id").ascending())).getContent();
    }

    @Transactional(readOnly = true)
    public long countPlayers() {
        return userRepository.countByRole(UserRole.PLAYER);
    }

    @Transactional(readOnly = true)
    public User findByLogin(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Nie znaleziono użytkownika: " + login));
    }

    private int safeSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }

    @Autowired(required = false)
    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @Autowired(required = false)
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
}
