package com.betclick.config;

import com.betclick.model.enums.UserRole;
import com.betclick.repository.SelectionRepository;
import com.betclick.repository.UserRepository;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

@Component
@Profile({"dev", "docker"})
public class DataSeederComponent implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeederComponent.class);

    private static final int COUPON_BATCH      = 50;
    private static final int COUPON_TOTAL      = 100;
    private static final int TRANSACTION_BATCH = 50;
    private static final int TRANSACTION_TOTAL = 100;

    private final DataSeederService   seederService;
    private final UserRepository      userRepository;
    private final SelectionRepository selectionRepository;
    private final PasswordEncoder     passwordEncoder;
    private final String              seedUserPassword;

    public DataSeederComponent(DataSeederService seederService,
                                UserRepository userRepository,
                                SelectionRepository selectionRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${betclick.seed.user-password:}") String seedUserPassword) {
        this.seederService       = seederService;
        this.userRepository      = userRepository;
        this.selectionRepository = selectionRepository;
        this.passwordEncoder     = passwordEncoder;
        this.seedUserPassword    = seedUserPassword;
    }

    @Override
    public void run(String... args) {

        if (!StringUtils.hasText(seedUserPassword)) {
            throw new IllegalStateException("BETCLICK_SEED_USER_PASSWORD must be set before demo users are seeded.");
        }
        String passwordHash = passwordEncoder.encode(seedUserPassword);

        long playerCount = userRepository.countByRole(UserRole.PLAYER);
        log.info("[DataSeeder] Strażnik: playerCount={}", playerCount);

        seederService.ensureDemoUsers(passwordHash);

        if (playerCount > 0) {
            log.info("[DataSeeder] Dane już istnieją — pomijam seedowanie.");
            return;
        }

        log.info("[DataSeeder] Startuję seedowanie danych...");
        long t0 = System.currentTimeMillis();

        Faker faker = new Faker(new Locale("pl"));

        seederService.seedUsers(faker, passwordHash);

        seederService.seedSports();

        seederService.seedLeagues(faker);

        List<Long> upcomingSelIds = new ArrayList<>();
        List<Long> finishedSelIds = new ArrayList<>();
        seederService.seedEventsMarketsSelections(faker, upcomingSelIds, finishedSelIds);

        Map<Long, BigDecimal> oddsCache   = new HashMap<>();
        Map<Long, Boolean>    winnerCache = new HashMap<>();
        selectionRepository.findAll().forEach(s -> {
            oddsCache.put(s.getId(), s.getOdds());
            winnerCache.put(s.getId(), s.getIsWinner());
        });

        List<Long> playerIds = userRepository.findAll().stream()
                .filter(u -> UserRole.PLAYER.equals(u.getRole()))
                .map(u -> u.getId())
                .toList();

        log.info("[DataSeeder] Seeduję {} kuponów (chunki po {})...", COUPON_TOTAL, COUPON_BATCH);
        for (int offset = 0; offset < COUPON_TOTAL; offset += COUPON_BATCH) {
            int chunk = Math.min(COUPON_BATCH, COUPON_TOTAL - offset);
            seederService.saveCouponChunk(faker, playerIds,
                    upcomingSelIds, finishedSelIds,
                    oddsCache, winnerCache,
                    offset, chunk);
            if (offset % 500 == 0) {
                log.info("[DataSeeder] Kupony: {}/{}", offset, COUPON_TOTAL);
            }
        }
        log.info("[DataSeeder] Kupony gotowe.");

        log.info("[DataSeeder] Seeduję {} dodatkowych transakcji...", TRANSACTION_TOTAL);
        for (int offset = 0; offset < TRANSACTION_TOTAL; offset += TRANSACTION_BATCH) {
            int chunk = Math.min(TRANSACTION_BATCH, TRANSACTION_TOTAL - offset);
            seederService.saveTransactionChunk(faker, playerIds, chunk);
        }
        log.info("[DataSeeder] Transakcje gotowe.");

        log.info("[DataSeeder] Seedowanie zakończone w {} ms.", System.currentTimeMillis() - t0);
    }
}
