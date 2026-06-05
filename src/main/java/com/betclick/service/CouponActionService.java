package com.betclick.service;

import com.betclick.model.Cashout;
import com.betclick.model.Coupon;
import com.betclick.model.CouponSelection;
import com.betclick.model.Transaction;
import com.betclick.model.User;
import com.betclick.model.enums.CouponStatus;
import com.betclick.model.enums.TransactionType;
import com.betclick.repository.CashoutRepository;
import com.betclick.repository.CouponRepository;
import com.betclick.repository.TransactionRepository;
import com.betclick.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class CouponActionService {

    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final CashoutRepository cashoutRepository;
    private final DictionaryService dictionaryService;
    private final NotificationService notificationService;
    private final RankingService rankingService;
    private final UserLevelService userLevelService;
    private final JdbcTemplate jdbcTemplate;

    public CouponActionService(CouponRepository couponRepository,
                               UserRepository userRepository,
                               TransactionRepository transactionRepository,
                               CashoutRepository cashoutRepository,
                               DictionaryService dictionaryService,
                               NotificationService notificationService,
                               RankingService rankingService,
                               UserLevelService userLevelService,
                               JdbcTemplate jdbcTemplate) {
        this.couponRepository = couponRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.cashoutRepository = cashoutRepository;
        this.dictionaryService = dictionaryService;
        this.notificationService = notificationService;
        this.rankingService = rankingService;
        this.userLevelService = userLevelService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public BigDecimal calculateCashoutAmount(Coupon coupon) {
        return coupon.getStake()
                .multiply(new BigDecimal("0.80"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isCashoutAvailable(Coupon coupon) {
        return coupon != null
                && CouponStatus.ACTIVE.equals(coupon.getStatus())
                && !cashoutRepository.existsByCouponId(coupon.getId());
    }

    public boolean isCancellationAvailable(Coupon coupon) {
        if (coupon == null || !CouponStatus.ACTIVE.equals(coupon.getStatus())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return coupon.getCouponSelections().stream()
                .map(CouponSelection::getSelection)
                .allMatch(selection -> selection.getMarket().getEvent().getStartTime().isAfter(now));
    }

    @Transactional
    public void cashout(String login, Long couponId) {
        User user = findUser(login);
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono kuponu."));
        ensureOwner(user, coupon);
        if (!CouponStatus.ACTIVE.equals(coupon.getStatus())) {
            throw new IllegalArgumentException("Cashout jest dostepny tylko dla aktywnych kuponow.");
        }
        if (cashoutRepository.existsByCouponId(couponId)) {
            throw new IllegalArgumentException("Cashout zostal juz wykonany dla tego kuponu.");
        }

        BigDecimal cashoutAmount = calculateCashoutAmount(coupon);
        LocalDateTime now = LocalDateTime.now();

        coupon.setStatus(CouponStatus.CASHED_OUT);
        coupon.setActualWin(cashoutAmount);
        coupon.setSettledAt(now);
        couponRepository.save(coupon);
        updateCouponStatusId(coupon.getId(), "CASHED_OUT");

        user.setBalance(user.getBalance().add(cashoutAmount));
        userRepository.save(user);

        cashoutRepository.save(Cashout.builder()
                .coupon(coupon)
                .user(user)
                .originalStake(coupon.getStake())
                .potentialWin(coupon.getPotentialWin())
                .cashoutAmount(cashoutAmount)
                .statusId(dictionaryService.id("cashout_statuses", "ACCEPTED"))
                .createdAt(now)
                .acceptedAt(now)
                .build());

        transactionRepository.save(Transaction.builder()
                .user(user)
                .coupon(coupon)
                .amount(cashoutAmount)
                .type(TransactionType.CASHOUT)
                .description("Cashout kuponu " + coupon.getTicketNumber())
                .createdAt(now)
                .build());

        rankingService.recordCashout(user, coupon.getStake(), cashoutAmount);
        userLevelService.updateLevel(user);
        notificationService.notify(
                user,
                "CASHOUT_ACCEPTED",
                "Cashout zaakceptowany",
                "Kupon " + coupon.getTicketNumber() + " zostal zamkniety. Wyplacono "
                        + cashoutAmount + " PLN.",
                "COUPON",
                coupon.getId()
        );
    }

    @Transactional
    public void cancelCoupon(String login, Long couponId) {
        User user = findUser(login);
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono kuponu."));
        ensureOwner(user, coupon);
        if (!CouponStatus.ACTIVE.equals(coupon.getStatus())) {
            throw new IllegalArgumentException("Anulowac mozna tylko aktywny kupon.");
        }
        if (!isCancellationAvailable(coupon)) {
            throw new IllegalArgumentException("Kuponu nie mozna anulowac po rozpoczeciu wydarzenia.");
        }

        LocalDateTime now = LocalDateTime.now();
        coupon.setStatus(CouponStatus.CANCELLED);
        coupon.setActualWin(BigDecimal.ZERO);
        coupon.setSettledAt(now);
        couponRepository.save(coupon);
        updateCouponStatusId(coupon.getId(), "CANCELLED");

        user.setBalance(user.getBalance().add(coupon.getStake()));
        userRepository.save(user);

        transactionRepository.save(Transaction.builder()
                .user(user)
                .coupon(coupon)
                .amount(coupon.getStake())
                .type(TransactionType.REFUND)
                .description("Zwrot stawki za anulowany kupon " + coupon.getTicketNumber())
                .createdAt(now)
                .build());

        notificationService.notify(
                user,
                "COUPON_CANCELLED",
                "Kupon anulowany",
                "Stawka za kupon " + coupon.getTicketNumber() + " wrocila na saldo.",
                "COUPON",
                coupon.getId()
        );
    }

    private User findUser(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono uzytkownika: " + login));
    }

    private void ensureOwner(User user, Coupon coupon) {
        if (!coupon.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Mozesz operowac tylko na wlasnych kuponach.");
        }
    }

    private void updateCouponStatusId(Long couponId, String statusCode) {
        jdbcTemplate.update(
                "UPDATE coupons SET status_id = ? WHERE id = ?",
                dictionaryService.id("coupon_statuses", statusCode),
                couponId
        );
    }
}
