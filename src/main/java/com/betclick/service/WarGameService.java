package com.betclick.service;

import com.betclick.dto.request.WarGameRequest;
import com.betclick.dto.response.WarGameRoundResponse;
import com.betclick.exception.InsufficientFundsException;
import com.betclick.model.Transaction;
import com.betclick.model.User;
import com.betclick.model.WarGameRound;
import com.betclick.model.enums.TransactionType;
import com.betclick.model.enums.WarOutcome;
import com.betclick.repository.TransactionRepository;
import com.betclick.repository.UserRepository;
import com.betclick.repository.WarGameRoundRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class WarGameService {

    private static final int HISTORY_LIMIT = 20;

    private final WarGameRoundRepository warGameRoundRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final DictionaryService dictionaryService;
    private final NotificationService notificationService;
    private final RankingService rankingService;
    private final UserLevelService userLevelService;

    public WarGameService(WarGameRoundRepository warGameRoundRepository,
                          UserRepository userRepository,
                          TransactionRepository transactionRepository,
                          DictionaryService dictionaryService,
                          NotificationService notificationService,
                          RankingService rankingService,
                          UserLevelService userLevelService) {
        this.warGameRoundRepository = warGameRoundRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.dictionaryService = dictionaryService;
        this.notificationService = notificationService;
        this.rankingService = rankingService;
        this.userLevelService = userLevelService;
    }

    @Transactional
    public WarGameRoundResponse play(String login, WarGameRequest request) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono uzytkownika: " + login));
        BigDecimal stake = normalizeStake(request.getStake());
        if (user.getBalance().compareTo(stake) < 0) {
            throw new InsufficientFundsException("Niewystarczajace srodki na gre.");
        }

        WarOutcome selectedOutcome = request.getSelectedOutcome();
        BigDecimal odds = oddsFor(selectedOutcome);
        WarOutcome actualOutcome = drawOutcome();
        Cards cards = drawCards(actualOutcome);
        boolean won = selectedOutcome == actualOutcome;
        BigDecimal payout = won ? stake.multiply(odds).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        user.setBalance(user.getBalance().subtract(stake).add(payout));
        userRepository.save(user);

        WarGameRound round = warGameRoundRepository.save(WarGameRound.builder()
                .user(user)
                .gameTypeId(dictionaryService.id("game_types", "WAR"))
                .stake(stake)
                .selectedOutcome(selectedOutcome)
                .actualOutcome(actualOutcome)
                .odds(odds)
                .payout(payout)
                .statusId(dictionaryService.id("game_round_statuses", won ? "WON" : "LOST"))
                .playerCard(cards.playerCard())
                .dealerCard(cards.dealerCard())
                .createdAt(now)
                .settledAt(now)
                .build());

        transactionRepository.save(Transaction.builder()
                .user(user)
                .amount(stake.negate())
                .type(TransactionType.GAME_BET)
                .description("Stawka w grze Wojna")
                .createdAt(now)
                .build());

        if (won) {
            transactionRepository.save(Transaction.builder()
                    .user(user)
                    .amount(payout)
                    .type(TransactionType.GAME_WIN)
                    .description("Wygrana w grze Wojna")
                    .createdAt(now)
                    .build());
        }

        rankingService.recordGameRound(user, stake, payout, won);
        userLevelService.updateLevel(user);
        notificationService.notify(
                user,
                "GAME_RESULT",
                won ? "Wygrana w grze Wojna" : "Przegrana w grze Wojna",
                "Wynik: " + actualOutcome + ". Twoja wyplata: " + payout + " PLN.",
                "WAR_GAME_ROUND",
                round.getId()
        );

        return map(round);
    }

    @Transactional(readOnly = true)
    public List<WarGameRoundResponse> history(String login) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono uzytkownika: " + login));
        return warGameRoundRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, HISTORY_LIMIT))
                .stream()
                .map(this::map)
                .toList();
    }

    private WarGameRoundResponse map(WarGameRound round) {
        return WarGameRoundResponse.builder()
                .id(round.getId())
                .stake(round.getStake())
                .selectedOutcome(round.getSelectedOutcome().name())
                .actualOutcome(round.getActualOutcome().name())
                .odds(round.getOdds())
                .payout(round.getPayout())
                .statusCode(dictionaryService.code("game_round_statuses", round.getStatusId()))
                .playerCard(round.getPlayerCard())
                .dealerCard(round.getDealerCard())
                .createdAt(round.getCreatedAt())
                .settledAt(round.getSettledAt())
                .build();
    }

    private WarOutcome drawOutcome() {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < 47) {
            return WarOutcome.PLAYER_WIN;
        }
        if (roll < 94) {
            return WarOutcome.DEALER_WIN;
        }
        return WarOutcome.WAR;
    }

    private Cards drawCards(WarOutcome outcome) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (outcome == WarOutcome.WAR) {
            int card = random.nextInt(2, 15);
            return new Cards(card, card);
        }

        int lowerCard = random.nextInt(2, 14);
        int higherCard = random.nextInt(lowerCard + 1, 15);
        if (outcome == WarOutcome.PLAYER_WIN) {
            return new Cards(higherCard, lowerCard);
        }
        return new Cards(lowerCard, higherCard);
    }

    private BigDecimal oddsFor(WarOutcome outcome) {
        return switch (outcome) {
            case PLAYER_WIN, DEALER_WIN -> new BigDecimal("2.00");
            case WAR -> new BigDecimal("10.00");
        };
    }

    private BigDecimal normalizeStake(BigDecimal stake) {
        if (stake == null || stake.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Stawka musi byc dodatnia.");
        }
        return stake.setScale(2, RoundingMode.HALF_UP);
    }

    private record Cards(int playerCard, int dealerCard) {
    }
}
