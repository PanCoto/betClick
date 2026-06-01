package com.betclick.service;

import com.betclick.dto.request.PlaceBetRequest;
import com.betclick.dto.response.CouponResponse;
import com.betclick.exception.BetNotAllowedException;
import com.betclick.exception.InsufficientFundsException;
import com.betclick.model.Coupon;
import com.betclick.model.Event;
import com.betclick.model.Market;
import com.betclick.model.Selection;
import com.betclick.model.enums.CouponStatus;
import com.betclick.model.enums.EventStatus;
import com.betclick.repository.CouponRepository;
import com.betclick.repository.SelectionRepository;
import com.betclick.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BettingService {

    private static final Logger log = LoggerFactory.getLogger(BettingService.class);
    private static final int DEFAULT_COUPON_LIMIT = 20;

    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final SelectionRepository selectionRepository;
    private final JdbcTemplate jdbcTemplate;
    private CouponActionService couponActionService;
    private UserLevelService userLevelService;

    public BettingService(CouponRepository couponRepository,
                          UserRepository userRepository,
                          SelectionRepository selectionRepository,
                          JdbcTemplate jdbcTemplate) {
        this.couponRepository = couponRepository;
        this.userRepository = userRepository;
        this.selectionRepository = selectionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public CouponResponse placeBet(String login, PlaceBetRequest request) {
        var user = userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika o loginie: " + login));

        validateSelectionsAreOpen(request.getSelectionIds());

        Long[] selIds = request.getSelectionIds().toArray(new Long[0]);

        try {
            return jdbcTemplate.execute((Connection conn) -> {
                try (CallableStatement call = conn.prepareCall("CALL place_bet(?, ?, ?, ?, ?, ?, ?)")) {
                    call.setLong(1, user.getId());
                    call.setArray(2, conn.createArrayOf("BIGINT", selIds));
                    call.setBigDecimal(3, request.getStake());
                    
                    call.registerOutParameter(4, Types.BIGINT);
                    call.registerOutParameter(5, Types.VARCHAR);
                    call.registerOutParameter(6, Types.DECIMAL);
                    call.registerOutParameter(7, Types.DECIMAL);

                    call.execute();

                    long couponId = call.getLong(4);
                    String ticketNumber = call.getString(5);
                    BigDecimal totalOdds = call.getBigDecimal(6);
                    BigDecimal potentialWin = call.getBigDecimal(7);

                    Coupon coupon = couponRepository.findById(couponId)
                            .orElseThrow(() -> new IllegalStateException("Nie udało się pobrać utworzonego kuponu"));

                    if (userLevelService != null) {
                        userLevelService.updateLevel(coupon.getUser());
                    }
                    return mapToCouponResponse(coupon);
                }
            });
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("Niewystarczające środki")) {
                throw new InsufficientFundsException("Niewystarczające środki na koncie gracza!");
            } else {
                log.warn("Could not place bet for user {}", login, e);
                throw new BetNotAllowedException("Nie mozna zawrzec zakladu. Sprawdz wybrane zdarzenia i sprobuj ponownie.");
            }
        }
    }

    private void validateSelectionsAreOpen(List<Long> selectionIds) {
        LocalDateTime now = LocalDateTime.now();

        for (Long selectionId : selectionIds) {
            Selection selection = selectionRepository.findById(selectionId)
                    .orElseThrow(() -> new BetNotAllowedException("Wybrana selekcja nie jest dostepna."));

            if (!Boolean.TRUE.equals(selection.getIsActive())) {
                throw new BetNotAllowedException("Wybrana selekcja nie jest juz dostepna.");
            }

            Market market = selection.getMarket();
            Event event = market != null ? market.getEvent() : null;
            if (event == null || event.getStatus() != EventStatus.UPCOMING) {
                throw new BetNotAllowedException("To wydarzenie nie jest juz dostepne do obstawienia.");
            }

            int expectedDurationMinutes = event.getExpectedDurationMinutes() != null ? event.getExpectedDurationMinutes() : 90;
            LocalDateTime plannedEndTime = event.getStartTime().plusMinutes(expectedDurationMinutes);
            if (!now.isBefore(plannedEndTime)) {
                throw new BetNotAllowedException("To wydarzenie juz sie zakonczylo i nie mozna go obstawic.");
            }
        }
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> getUserCoupons(String login) {
        var user = userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika o loginie: " + login));

        return couponRepository.findByUserIdOrderByPlacedAtDesc(user.getId(), PageRequest.of(0, DEFAULT_COUPON_LIMIT)).getContent().stream()
                .map(this::mapToCouponResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> getRecentUserCoupons(String login, int limit) {
        var user = userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika o loginie: " + login));

        return couponRepository.findByUserIdOrderByPlacedAtDesc(user.getId(), PageRequest.of(0, safeSize(limit))).getContent().stream()
                .map(this::mapToCouponResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<CouponResponse> getUserCouponsPage(String login, Pageable pageable) {
        var user = userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika o loginie: " + login));
        return couponRepository.findByUserIdOrderByPlacedAtDesc(user.getId(), pageable)
                .map(this::mapToCouponResponse);
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> getUserCouponsByStatus(String login, String statusStr) {
        var user = userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika o loginie: " + login));

        CouponStatus status = CouponStatus.valueOf(statusStr.toUpperCase());
        return couponRepository.findByUserIdAndStatusOrderByPlacedAtDesc(user.getId(), status, PageRequest.of(0, DEFAULT_COUPON_LIMIT)).stream()
                .map(this::mapToCouponResponse)
                .collect(Collectors.toList());
    }

    private int safeSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }

    @Transactional(readOnly = true)
    public CouponResponse getCouponDetails(String login, Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono kuponu o ID: " + couponId));

        if (!coupon.getUser().getLogin().equals(login) && !coupon.getUser().getRole().name().equals("ADMIN") && !coupon.getUser().getRole().name().equals("MODERATOR")) {
            throw new SecurityException("Brak uprawnień do przeglądania tego kuponu!");
        }

        return mapToCouponResponse(coupon);
    }

    public CouponResponse mapToCouponResponse(Coupon coupon) {
        List<CouponResponse.SelectionDetail> selections = coupon.getCouponSelections().stream().map(cs -> {
            var event = cs.getSelection().getMarket().getEvent();
            int expectedDurationMinutes = event.getExpectedDurationMinutes() != null ? event.getExpectedDurationMinutes() : 90;

            return CouponResponse.SelectionDetail.builder()
                    .selectionId(cs.getSelection().getId())
                    .selectionName(cs.getSelection().getName())
                    .oddsAtPlacement(cs.getOddsAtPlacement())
                    .marketName(cs.getSelection().getMarket().getName())
                    .eventName(event.getName())
                    .eventStartTime(event.getStartTime())
                    .eventExpectedDurationMinutes(expectedDurationMinutes)
                    .eventEndTime(event.getStartTime().plusMinutes(expectedDurationMinutes))
                    .isWinner(cs.getSelection().getIsWinner())
                    .build();
        }).collect(Collectors.toList());

        return CouponResponse.builder()
                .id(coupon.getId())
                .ticketNumber(coupon.getTicketNumber())
                .stake(coupon.getStake())
                .totalOdds(coupon.getTotalOdds())
                .potentialWin(coupon.getPotentialWin())
                .actualWin(coupon.getActualWin())
                .status(coupon.getStatus().name())
                .cashoutAmount(calculateCashoutAmount(coupon))
                .cashoutAvailable(isCashoutAvailable(coupon))
                .cancellationAvailable(isCancellationAvailable(coupon))
                .placedAt(coupon.getPlacedAt())
                .settledAt(coupon.getSettledAt())
                .selections(selections)
                .build();
    }

    private BigDecimal calculateCashoutAmount(Coupon coupon) {
        if (couponActionService != null) {
            return couponActionService.calculateCashoutAmount(coupon);
        }
        return coupon.getStake().multiply(new BigDecimal("0.80"));
    }

    private boolean isCashoutAvailable(Coupon coupon) {
        return couponActionService != null && couponActionService.isCashoutAvailable(coupon);
    }

    private boolean isCancellationAvailable(Coupon coupon) {
        return couponActionService != null && couponActionService.isCancellationAvailable(coupon);
    }

    @Autowired(required = false)
    public void setCouponActionService(CouponActionService couponActionService) {
        this.couponActionService = couponActionService;
    }

    @Autowired(required = false)
    public void setUserLevelService(UserLevelService userLevelService) {
        this.userLevelService = userLevelService;
    }
}
