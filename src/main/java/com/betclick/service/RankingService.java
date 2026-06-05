package com.betclick.service;

import com.betclick.dto.response.PlayerRankingResponse;
import com.betclick.model.PlayerRanking;
import com.betclick.model.User;
import com.betclick.repository.PlayerRankingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RankingService {

    private final PlayerRankingRepository rankingRepository;

    public RankingService(PlayerRankingRepository rankingRepository) {
        this.rankingRepository = rankingRepository;
    }

    @Transactional
    public void recordCouponSettlement(User user, BigDecimal stake, BigDecimal payout, boolean won) {
        PlayerRanking ranking = rankingFor(user);
        ranking.setTotalStaked(value(ranking.getTotalStaked()).add(value(stake)));
        ranking.setTotalWon(value(ranking.getTotalWon()).add(value(payout)));
        ranking.setTotalProfit(value(ranking.getTotalProfit()).add(value(payout)).subtract(value(stake)));
        if (won) {
            ranking.setWonCouponsCount(safeLong(ranking.getWonCouponsCount()) + 1);
        } else {
            ranking.setLostCouponsCount(safeLong(ranking.getLostCouponsCount()) + 1);
        }
        ranking.setUpdatedAt(LocalDateTime.now());
        rankingRepository.save(ranking);
    }

    @Transactional
    public void recordStake(User user, BigDecimal stake) {
        PlayerRanking ranking = rankingFor(user);
        ranking.setTotalStaked(value(ranking.getTotalStaked()).add(value(stake)));
        ranking.setTotalProfit(value(ranking.getTotalProfit()).subtract(value(stake)));
        ranking.setUpdatedAt(LocalDateTime.now());
        rankingRepository.save(ranking);
    }

    @Transactional
    public void recordCashout(User user, BigDecimal stake, BigDecimal payout) {
        PlayerRanking ranking = rankingFor(user);
        ranking.setTotalStaked(value(ranking.getTotalStaked()).add(value(stake)));
        ranking.setTotalWon(value(ranking.getTotalWon()).add(value(payout)));
        ranking.setTotalProfit(value(ranking.getTotalProfit()).add(value(payout)).subtract(value(stake)));
        ranking.setUpdatedAt(LocalDateTime.now());
        rankingRepository.save(ranking);
    }

    @Transactional
    public void recordGameRound(User user, BigDecimal stake, BigDecimal payout, boolean won) {
        PlayerRanking ranking = rankingFor(user);
        ranking.setTotalStaked(value(ranking.getTotalStaked()).add(value(stake)));
        ranking.setTotalWon(value(ranking.getTotalWon()).add(value(payout)));
        ranking.setTotalProfit(value(ranking.getTotalProfit()).add(value(payout)).subtract(value(stake)));
        if (won) {
            ranking.setGamesWonCount(safeLong(ranking.getGamesWonCount()) + 1);
        } else {
            ranking.setGamesLostCount(safeLong(ranking.getGamesLostCount()) + 1);
        }
        ranking.setUpdatedAt(LocalDateTime.now());
        rankingRepository.save(ranking);
    }

    @Transactional(readOnly = true)
    public List<PlayerRankingResponse> top(int limit) {
        AtomicInteger place = new AtomicInteger(1);
        return rankingRepository.findByOrderByTotalWonDescTotalProfitDesc(PageRequest.of(0, Math.max(1, Math.min(limit, 50))))
                .stream()
                .map(ranking -> map(ranking, place.getAndIncrement()))
                .toList();
    }

    private PlayerRanking rankingFor(User user) {
        return rankingRepository.findByUserId(user.getId())
                .orElseGet(() -> PlayerRanking.builder().user(user).build());
    }

    private PlayerRankingResponse map(PlayerRanking ranking, int place) {
        return PlayerRankingResponse.builder()
                .place(place)
                .login(ranking.getUser().getLogin())
                .totalStaked(value(ranking.getTotalStaked()))
                .totalWon(value(ranking.getTotalWon()))
                .totalProfit(value(ranking.getTotalProfit()))
                .wonCouponsCount(safeLong(ranking.getWonCouponsCount()))
                .lostCouponsCount(safeLong(ranking.getLostCouponsCount()))
                .gamesWonCount(safeLong(ranking.getGamesWonCount()))
                .gamesLostCount(safeLong(ranking.getGamesLostCount()))
                .updatedAt(ranking.getUpdatedAt())
                .build();
    }

    private BigDecimal value(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private long safeLong(Long value) {
        return value != null ? value : 0L;
    }
}
