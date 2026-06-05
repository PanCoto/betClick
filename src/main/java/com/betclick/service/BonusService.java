package com.betclick.service;

import com.betclick.dto.response.BonusResponse;
import com.betclick.model.Bonus;
import com.betclick.model.User;
import com.betclick.repository.BonusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BonusService {

    private static final BigDecimal WELCOME_AMOUNT = new BigDecimal("100.00");

    private final BonusRepository bonusRepository;
    private final DictionaryService dictionaryService;
    private final NotificationService notificationService;

    public BonusService(BonusRepository bonusRepository,
                        DictionaryService dictionaryService,
                        NotificationService notificationService) {
        this.bonusRepository = bonusRepository;
        this.dictionaryService = dictionaryService;
        this.notificationService = notificationService;
    }

    @Transactional
    public void grantWelcomeBonus(User user) {
        Long typeId = dictionaryService.id("bonus_types", "WELCOME_BONUS");
        if (user.getId() == null || bonusRepository.existsByUserIdAndBonusTypeId(user.getId(), typeId)) {
            return;
        }

        Bonus bonus = bonusRepository.save(Bonus.builder()
                .user(user)
                .bonusTypeId(typeId)
                .statusId(dictionaryService.id("bonus_statuses", "AVAILABLE"))
                .amount(WELCOME_AMOUNT)
                .remainingAmount(WELCOME_AMOUNT)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build());

        notificationService.notify(
                user,
                "BONUS_GRANTED",
                "Bonus powitalny",
                "Otrzymales bonus powitalny 100 zl.",
                "BONUS",
                bonus.getId()
        );
    }

    @Transactional(readOnly = true)
    public List<BonusResponse> getUserBonuses(User user) {
        return bonusRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::map)
                .toList();
    }

    private BonusResponse map(Bonus bonus) {
        return BonusResponse.builder()
                .id(bonus.getId())
                .typeCode(dictionaryService.code("bonus_types", bonus.getBonusTypeId()))
                .statusCode(dictionaryService.code("bonus_statuses", bonus.getStatusId()))
                .amount(bonus.getAmount())
                .remainingAmount(bonus.getRemainingAmount())
                .createdAt(bonus.getCreatedAt())
                .usedAt(bonus.getUsedAt())
                .expiresAt(bonus.getExpiresAt())
                .build();
    }
}
