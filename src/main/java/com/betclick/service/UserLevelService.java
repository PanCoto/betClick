package com.betclick.service;

import com.betclick.model.User;
import com.betclick.model.UserLevel;
import com.betclick.repository.UserLevelRepository;
import com.betclick.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class UserLevelService {

    private final UserRepository userRepository;
    private final UserLevelRepository userLevelRepository;
    private final JdbcTemplate jdbcTemplate;
    private final NotificationService notificationService;

    public UserLevelService(UserRepository userRepository,
                            UserLevelRepository userLevelRepository,
                            JdbcTemplate jdbcTemplate,
                            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.userLevelRepository = userLevelRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.notificationService = notificationService;
    }

    @Transactional
    public void updateLevel(User user) {
        if (user == null || user.getId() == null) {
            return;
        }

        BigDecimal totalStake = loadTotalStake(user.getId());
        UserLevel nextLevel = userLevelRepository.findTopByMinTotalStakeLessThanEqualOrderBySortOrderDesc(totalStake)
                .orElseGet(() -> userLevelRepository.findByCode("BRONZE")
                        .orElseThrow(() -> new IllegalStateException("Brak poziomu BRONZE w slowniku user_levels.")));

        if (!nextLevel.getId().equals(user.getLevelId())) {
            user.setLevelId(nextLevel.getId());
            userRepository.save(user);
            notificationService.notify(
                    user,
                    "LEVEL_CHANGED",
                    "Nowy poziom gracza",
                    "Twoj poziom to teraz " + nextLevel.getName() + ".",
                    "USER_LEVEL",
                    nextLevel.getId()
            );
        }
    }

    private BigDecimal loadTotalStake(Long userId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT " +
                            "COALESCE((SELECT SUM(stake) FROM coupons WHERE user_id = ?), 0.00) + " +
                            "COALESCE((SELECT SUM(stake) FROM war_game_rounds WHERE user_id = ?), 0.00)",
                    BigDecimal.class,
                    userId,
                    userId
            );
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
