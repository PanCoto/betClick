package com.betclick.config;

import com.betclick.model.*;
import com.betclick.model.enums.*;
import com.betclick.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DataSeederService {

    private static final Logger log = LoggerFactory.getLogger(DataSeederService.class);

    private static final int USER_BATCH        = 50;
    private static final int EVENT_BATCH       = 25;
    private static final int COUPON_BATCH      = 100;
    private static final int TRANSACTION_BATCH = 200;
    private static final BigDecimal DEMO_PLAYER_BALANCE = new BigDecimal("1000.00");

    private final UserRepository            userRepository;
    private final SportRepository           sportRepository;
    private final LeagueRepository          leagueRepository;
    private final EventRepository           eventRepository;
    private final MarketRepository          marketRepository;
    private final SelectionRepository       selectionRepository;
    private final CouponRepository          couponRepository;
    private final CouponSelectionRepository couponSelectionRepository;
    private final TransactionRepository     transactionRepository;

    @PersistenceContext
    private EntityManager em;

    public DataSeederService(UserRepository userRepository,
                              SportRepository sportRepository,
                              LeagueRepository leagueRepository,
                              EventRepository eventRepository,
                              MarketRepository marketRepository,
                              SelectionRepository selectionRepository,
                              CouponRepository couponRepository,
                              CouponSelectionRepository couponSelectionRepository,
                              TransactionRepository transactionRepository) {
        this.userRepository            = userRepository;
        this.sportRepository           = sportRepository;
        this.leagueRepository          = leagueRepository;
        this.eventRepository           = eventRepository;
        this.marketRepository          = marketRepository;
        this.selectionRepository       = selectionRepository;
        this.couponRepository          = couponRepository;
        this.couponSelectionRepository = couponSelectionRepository;
        this.transactionRepository     = transactionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureDemoUsers(String passwordHash) {
        User admin = userRepository.findByLogin("admin")
                .orElseGet(() -> User.builder()
                        .login("admin")
                        .email("admin@betclick.pl")
                        .firstName("Administrator")
                        .lastName("System")
                        .dateOfBirth(LocalDate.of(1990, 1, 1))
                        .balance(BigDecimal.ZERO)
                        .registrationDate(LocalDateTime.now().minusMonths(6))
                        .build());
        admin.setPasswordHash(passwordHash);
        admin.setIsActive(true);
        admin.setRole(UserRole.ADMIN);
        if (!StringUtils.hasText(admin.getEmail())) {
            admin.setEmail("admin@betclick.pl");
        }
        if (!StringUtils.hasText(admin.getFirstName())) {
            admin.setFirstName("Administrator");
        }
        if (!StringUtils.hasText(admin.getLastName())) {
            admin.setLastName("System");
        }
        if (admin.getDateOfBirth() == null) {
            admin.setDateOfBirth(LocalDate.of(1990, 1, 1));
        }
        if (admin.getRegistrationDate() == null) {
            admin.setRegistrationDate(LocalDateTime.now().minusMonths(6));
        }
        if (admin.getBalance() == null) {
            admin.setBalance(BigDecimal.ZERO);
        }
        userRepository.save(admin);

        User player = userRepository.findByLogin("player")
                .orElseGet(() -> User.builder()
                        .login("player")
                        .email("player@betclick.pl")
                        .firstName("Jan")
                        .lastName("Kowalski")
                        .dateOfBirth(LocalDate.of(1995, 5, 15))
                        .phoneNumber("123456789")
                        .balance(new BigDecimal("1000.00"))
                        .registrationDate(LocalDateTime.now().minusMonths(6))
                        .build());
        player.setPasswordHash(passwordHash);
        player.setIsActive(true);
        player.setRole(UserRole.PLAYER);
        if (!StringUtils.hasText(player.getEmail())) {
            player.setEmail("player@betclick.pl");
        }
        if (!StringUtils.hasText(player.getFirstName())) {
            player.setFirstName("Jan");
        }
        if (!StringUtils.hasText(player.getLastName())) {
            player.setLastName("Kowalski");
        }
        if (!StringUtils.hasText(player.getPhoneNumber())) {
            player.setPhoneNumber("123456789");
        }
        if (player.getDateOfBirth() == null) {
            player.setDateOfBirth(LocalDate.of(1995, 5, 15));
        }
        if (player.getRegistrationDate() == null) {
            player.setRegistrationDate(LocalDateTime.now().minusMonths(6));
        }
        if (player.getBalance() == null || player.getBalance().compareTo(DEMO_PLAYER_BALANCE) < 0) {
            player.setBalance(DEMO_PLAYER_BALANCE);
        }
        userRepository.save(player);

        em.flush();
        em.clear();
        log.info("[DataSeeder] Demo users admin/player are ready.");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void seedUsers(Faker faker, String passwordHash) {
        List<User> batch = new ArrayList<>();

        if (!userRepository.existsByLogin("admin")) {
            batch.add(User.builder()
                    .login("admin")
                    .passwordHash(passwordHash)
                    .email("admin@betclick.pl")
                    .firstName("Administrator")
                    .lastName("System")
                    .dateOfBirth(LocalDate.of(1990, 1, 1))
                    .balance(BigDecimal.ZERO)
                    .isActive(true)
                    .role(UserRole.ADMIN)
                    .registrationDate(LocalDateTime.now().minusMonths(6))
                    .build());
        }

        if (!userRepository.existsByLogin("player")) {
            batch.add(User.builder()
                    .login("player")
                    .passwordHash(passwordHash)
                    .email("player@betclick.pl")
                    .firstName("Jan")
                    .lastName("Kowalski")
                    .dateOfBirth(LocalDate.of(1995, 5, 15))
                    .phoneNumber("123456789")
                    .balance(new BigDecimal("1000.00"))
                    .isActive(true)
                    .role(UserRole.PLAYER)
                    .registrationDate(LocalDateTime.now().minusMonths(6))
                    .build());
        }

        for (int i = 0; i < 99; i++) {
            String login = faker.internet().username() + "_" + i;
            if (login.length() > 50) login = login.substring(0, 50);
            String email = i + "_" + faker.internet().emailAddress();
            if (email.length() > 100) email = email.substring(0, 100);

            batch.add(User.builder()
                    .login(login)
                    .passwordHash(passwordHash)
                    .email(email)
                    .firstName(faker.name().firstName())
                    .lastName(faker.name().lastName())
                    .dateOfBirth(LocalDate.of(
                            1960 + faker.random().nextInt(45),
                            faker.random().nextInt(12) + 1,
                            faker.random().nextInt(28) + 1))
                    .phoneNumber(faker.phoneNumber().cellPhone())
                    .balance(BigDecimal.valueOf(faker.number().randomDouble(2, 50, 5000))
                            .setScale(2, RoundingMode.HALF_UP))
                    .isActive(faker.random().nextInt(100) < 95)
                    .role(faker.random().nextInt(100) < 1 ? UserRole.MODERATOR : UserRole.PLAYER)
                    .registrationDate(LocalDateTime.now().minusDays(faker.number().numberBetween(10, 300)))
                    .build());

            if (batch.size() >= USER_BATCH) {
                userRepository.saveAll(batch);
                em.flush();
                em.clear();
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            userRepository.saveAll(batch);
            em.flush();
            em.clear();
        }
        log.info("[DataSeeder] Users seeded.");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void seedSports() {
        String[] names = {"Piłka Nożna", "Tenis", "Koszykówka", "Siatkówka",
                          "Hokej", "Boks", "MMA", "Żużel"};
        String[] cats  = {"Sporty Drużynowe", "Sporty Indywidualne", "Sporty Drużynowe",
                          "Sporty Drużynowe", "Sporty Drużynowe", "Sporty Walki",
                          "Sporty Walki", "Sporty Motorowe"};

        int inserted = 0;
        for (int i = 0; i < names.length; i++) {
            if (sportRepository.findByName(names[i]).isEmpty()) {
                sportRepository.save(
                        Sport.builder().name(names[i]).category(cats[i]).isActive(true).build());
                inserted++;
            }
        }
        em.flush();
        em.clear();
        log.info("[DataSeeder] Sports seeded: {} nowych (reszta już istniała).", inserted);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void seedLeagues(Faker faker) {
        List<Sport> sports = sportRepository.findAll();
        Map<String, String[]> map = new LinkedHashMap<>();
        map.put("Piłka Nożna",  new String[]{"PKO BP Ekstraklasa", "Premier League", "La Liga", "Serie A", "Champions League"});
        map.put("Tenis",        new String[]{"Wimbledon", "US Open", "Roland Garros", "Australian Open"});
        map.put("Koszykówka",   new String[]{"NBA", "EuroLeague", "PLK"});
        map.put("Siatkówka",    new String[]{"PlusLiga", "Liga Narodów"});
        map.put("Hokej",        new String[]{"NHL", "PHL"});
        map.put("Boks",         new String[]{"WBC Heavyweight Championship", "WBA Title Fight"});
        map.put("MMA",          new String[]{"UFC", "KSW"});
        map.put("Żużel",        new String[]{"Ekstraliga żużlowa", "Grand Prix"});

        List<League> batch = new ArrayList<>();
        for (Sport sport : sports) {
            String[] names = map.getOrDefault(sport.getName(),
                    new String[]{sport.getName() + " League"});
            for (String name : names) {
                batch.add(League.builder()
                        .sport(sport).name(name)
                        .country(faker.address().country())
                        .season("2024/25").isActive(true).build());
            }
        }
        leagueRepository.saveAll(batch);
        em.flush();
        em.clear();
        log.info("[DataSeeder] Leagues seeded: {}", batch.size());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void seedEventsMarketsSelections(Faker faker,
                                             List<Long> upcomingSelIds,
                                             List<Long> finishedSelIds) {
        List<League> leagues = leagueRepository.findAllWithSport();

        List<Event> eventBatch = new ArrayList<>();
        for (int i = 0; i < 80; i++)
            eventBatch.add(buildEvent(faker, leagues, EventStatus.UPCOMING,
                    LocalDateTime.now().plusHours(faker.number().numberBetween(2, 480)), null, null));
        for (int i = 0; i < 10; i++)
            eventBatch.add(buildEvent(faker, leagues, EventStatus.LIVE,
                    LocalDateTime.now().minusMinutes(faker.number().numberBetween(5, 80)), null, null));
        for (int i = 0; i < 10; i++) {
            int rA = faker.number().numberBetween(0, 5);
            int rB = faker.number().numberBetween(0, 5);
            eventBatch.add(buildEvent(faker, leagues, EventStatus.FINISHED,
                    LocalDateTime.now().minusDays(faker.number().numberBetween(1, 30)), rA, rB));
        }
        eventRepository.saveAll(eventBatch);
        em.flush();
        em.clear();

        List<Event> allEvents = eventRepository.findAllWithLeagueSport();

        int counter = 0;
        for (Event event : allEvents) {
            String sportName   = event.getLeague().getSport().getName();
            boolean drawOk     = !sportName.equals("Tenis") && !sportName.equals("Boks") && !sportName.equals("MMA");
            boolean isFinished = event.getStatus() == EventStatus.FINISHED;

            Market m1 = marketRepository.save(Market.builder()
                    .event(event).name("Zwycięzca meczu")
                    .description("Typowanie wyniku końcowego spotkania")
                    .isSettled(isFinished).isActive(true).build());

            Selection s1 = selectionRepository.save(Selection.builder().market(m1)
                    .name("1 (" + event.getTeamA() + ")").odds(safeOdds(faker, 1, 6))
                    .isWinner(isFinished ? event.getResultA() > event.getResultB() : null)
                    .isActive(true).build());
            addPool(s1, event.getStatus(), upcomingSelIds, finishedSelIds);

            if (drawOk) {
                Selection sX = selectionRepository.save(Selection.builder().market(m1)
                        .name("X (Remis)").odds(safeOdds(faker, 2, 4))
                        .isWinner(isFinished ? Objects.equals(event.getResultA(), event.getResultB()) : null)
                        .isActive(true).build());
                addPool(sX, event.getStatus(), upcomingSelIds, finishedSelIds);
            }

            Selection s2 = selectionRepository.save(Selection.builder().market(m1)
                    .name("2 (" + event.getTeamB() + ")").odds(safeOdds(faker, 1, 6))
                    .isWinner(isFinished ? event.getResultA() < event.getResultB() : null)
                    .isActive(true).build());
            addPool(s2, event.getStatus(), upcomingSelIds, finishedSelIds);

            Market m2 = marketRepository.save(Market.builder()
                    .event(event).name("Powyżej/Poniżej 2.5 goli/punktów")
                    .description("Łączna liczba goli/punktów w meczu")
                    .isSettled(isFinished).isActive(true).build());

            boolean overWon = isFinished && (event.getResultA() + event.getResultB()) > 2;
            Selection sOver = selectionRepository.save(Selection.builder().market(m2)
                    .name("Powyżej 2.5").odds(safeOdds(faker, 1, 3))
                    .isWinner(isFinished ? overWon : null).isActive(true).build());
            Selection sUnder = selectionRepository.save(Selection.builder().market(m2)
                    .name("Poniżej 2.5").odds(safeOdds(faker, 1, 3))
                    .isWinner(isFinished ? !overWon : null).isActive(true).build());
            addPool(sOver,  event.getStatus(), upcomingSelIds, finishedSelIds);
            addPool(sUnder, event.getStatus(), upcomingSelIds, finishedSelIds);

            if (++counter % EVENT_BATCH == 0) {
                em.flush();
                em.clear();
            }
        }
        em.flush();
        em.clear();
        log.info("[DataSeeder] Events/Markets/Selections seeded. upcoming={}, finished={}",
                upcomingSelIds.size(), finishedSelIds.size());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveCouponChunk(Faker faker,
                                 List<Long> playerIds,
                                 List<Long> upcomingSelIds,
                                 List<Long> finishedSelIds,
                                 Map<Long, BigDecimal> oddsCache,
                                 Map<Long, Boolean> winnerCache,
                                 int globalOffset,
                                 int chunkSize) {
        for (int i = 0; i < chunkSize; i++) {
            int idx       = globalOffset + i;
            boolean isFin = (idx < 3000) && !finishedSelIds.isEmpty();
            List<Long> pool = isFin ? finishedSelIds : upcomingSelIds;
            if (pool.isEmpty()) pool = new ArrayList<>(oddsCache.keySet());

            Long userId = playerIds.get(faker.random().nextInt(playerIds.size()));
            User user   = userRepository.getReferenceById(userId);

            BigDecimal stake = BigDecimal.valueOf(faker.number().randomDouble(2, 5, 200))
                    .setScale(2, RoundingMode.HALF_UP);

            Set<Long> chosen = new LinkedHashSet<>();
            int want = faker.number().numberBetween(1, 4);
            for (int k = 0; k < want * 5 && chosen.size() < want; k++) {
                chosen.add(pool.get(faker.random().nextInt(pool.size())));
            }
            if (chosen.isEmpty()) chosen.add(pool.get(0));

            BigDecimal totalOdds = BigDecimal.ONE;
            for (Long sid : chosen) {
                totalOdds = totalOdds.multiply(oddsCache.getOrDefault(sid, new BigDecimal("1.50")));
            }
            totalOdds = totalOdds.setScale(2, RoundingMode.HALF_UP);
            BigDecimal potentialWin = stake.multiply(totalOdds).setScale(2, RoundingMode.HALF_UP);

            CouponStatus  status    = CouponStatus.ACTIVE;
            BigDecimal    actualWin = BigDecimal.ZERO;
            LocalDateTime settledAt = null;

            if (isFin) {
                boolean allWon = chosen.stream()
                        .allMatch(sid -> Boolean.TRUE.equals(winnerCache.get(sid)));
                status    = allWon ? CouponStatus.WON : CouponStatus.LOST;
                actualWin = allWon ? potentialWin : BigDecimal.ZERO;
                settledAt = LocalDateTime.now().minusDays(faker.number().numberBetween(1, 10));
            }

            String        ticketNumber = "BET-" + String.format("%08d", idx + 1);
            LocalDateTime placedAt     = isFin
                    ? settledAt.minusHours(faker.number().numberBetween(1, 48))
                    : LocalDateTime.now().minusHours(faker.number().numberBetween(1, 72));

            Coupon coupon = couponRepository.save(Coupon.builder()
                    .user(user).ticketNumber(ticketNumber).stake(stake)
                    .totalOdds(totalOdds).potentialWin(potentialWin)
                    .actualWin(actualWin).status(status)
                    .placedAt(placedAt).settledAt(settledAt).build());

            for (Long sid : chosen) {
                couponSelectionRepository.save(CouponSelection.builder()
                        .coupon(coupon)
                        .selection(selectionRepository.getReferenceById(sid))
                        .oddsAtPlacement(oddsCache.getOrDefault(sid, new BigDecimal("1.50")))
                        .build());
            }

            transactionRepository.save(Transaction.builder()
                    .user(user).coupon(coupon).amount(stake.negate())
                    .type(TransactionType.BET)
                    .description("Zakład na kupon " + ticketNumber)
                    .createdAt(placedAt).build());

            if (status == CouponStatus.WON) {
                transactionRepository.save(Transaction.builder()
                        .user(user).coupon(coupon).amount(actualWin)
                        .type(TransactionType.WIN)
                        .description("Wygrana z kuponu " + ticketNumber)
                        .createdAt(settledAt).build());
            }
        }
        em.flush();
        em.clear();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveTransactionChunk(Faker faker, List<Long> playerIds, int chunkSize) {
        for (int i = 0; i < chunkSize; i++) {
            Long userId       = playerIds.get(faker.random().nextInt(playerIds.size()));
            User user         = userRepository.getReferenceById(userId);
            boolean isDeposit = faker.random().nextInt(100) < 80;
            BigDecimal amount = BigDecimal.valueOf(faker.number().randomDouble(2, 10, 2000))
                    .setScale(2, RoundingMode.HALF_UP);

            transactionRepository.save(Transaction.builder()
                    .user(user)
                    .amount(isDeposit ? amount : amount.negate())
                    .type(isDeposit ? TransactionType.DEPOSIT : TransactionType.WITHDRAWAL)
                    .description(isDeposit ? "Doładowanie konta" : "Wypłata środków")
                    .createdAt(LocalDateTime.now().minusDays(faker.number().numberBetween(1, 180)))
                    .build());
        }
        em.flush();
        em.clear();
    }

    private Event buildEvent(Faker faker, List<League> leagues,
                              EventStatus status, LocalDateTime startTime,
                              Integer rA, Integer rB) {
        League league = leagues.get(faker.random().nextInt(leagues.size()));
        String teamA  = faker.company().name();
        String teamB  = faker.company().name();
        while (teamA.equals(teamB)) teamB = faker.company().name();
        return Event.builder()
                .league(league).name(teamA + " vs " + teamB)
                .teamA(teamA).teamB(teamB).startTime(startTime)
                .status(status).resultA(rA).resultB(rB)
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now()).build();
    }

    private BigDecimal safeOdds(Faker faker, int min, int max) {
        BigDecimal o = BigDecimal.valueOf(faker.number().randomDouble(2, min, max))
                .setScale(2, RoundingMode.HALF_UP);
        return o.compareTo(BigDecimal.ONE) <= 0 ? new BigDecimal("1.05") : o;
    }

    private void addPool(Selection s, EventStatus status,
                          List<Long> upcoming, List<Long> finished) {
        if (status == EventStatus.UPCOMING || status == EventStatus.LIVE)
            upcoming.add(s.getId());
        else if (status == EventStatus.FINISHED)
            finished.add(s.getId());
    }
}
