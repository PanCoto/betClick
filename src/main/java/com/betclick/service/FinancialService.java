package com.betclick.service;

import com.betclick.dto.response.TransactionResponse;
import com.betclick.exception.InsufficientFundsException;
import com.betclick.model.Transaction;
import com.betclick.repository.TransactionRepository;
import com.betclick.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FinancialService {

    private static final Logger log = LoggerFactory.getLogger(FinancialService.class);
    private static final int DEFAULT_TRANSACTION_LIMIT = 20;

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public FinancialService(TransactionRepository transactionRepository, UserRepository userRepository, JdbcTemplate jdbcTemplate) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public BigDecimal deposit(String login, BigDecimal amount) {
        var user = userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika o loginie: " + login));

        try {
            return jdbcTemplate.execute((Connection conn) -> {
                try (CallableStatement call = conn.prepareCall("CALL deposit_funds(?, ?, ?)")) {
                    call.setLong(1, user.getId());
                    call.setBigDecimal(2, amount);
                    call.registerOutParameter(3, Types.DECIMAL);

                    call.execute();
                    return call.getBigDecimal(3);
                }
            });
        } catch (Exception e) {
            log.warn("Could not deposit funds for user {}", login, e);
            throw new IllegalArgumentException("Wplata nie powiodla sie. Sprobuj ponownie pozniej.");
        }
    }

    @Transactional
    public BigDecimal withdraw(String login, BigDecimal amount) {
        var user = userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika o loginie: " + login));

        try {
            return jdbcTemplate.execute((Connection conn) -> {
                try (CallableStatement call = conn.prepareCall("CALL withdraw_funds(?, ?, ?)")) {
                    call.setLong(1, user.getId());
                    call.setBigDecimal(2, amount);
                    call.registerOutParameter(3, Types.DECIMAL);

                    call.execute();
                    return call.getBigDecimal(3);
                }
            });
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("Niewystarczające środki")) {
                throw new InsufficientFundsException("Niewystarczające środki na koncie gracza!");
            }
            log.warn("Could not withdraw funds for user {}", login, e);
            throw new IllegalArgumentException("Wyplata nie powiodla sie. Sprobuj ponownie pozniej.");
        }
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(String login) {
        var user = userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika o loginie: " + login));

        return transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, DEFAULT_TRANSACTION_LIMIT)).stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getRecentTransactionHistory(String login, int limit) {
        var user = userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika o loginie: " + login));

        return transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, safeSize(limit))).stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    private int safeSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }

    private TransactionResponse mapToTransactionResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .userId(tx.getUser().getId())
                .couponId(tx.getCoupon() != null ? tx.getCoupon().getId() : null)
                .couponTicketNumber(tx.getCoupon() != null ? tx.getCoupon().getTicketNumber() : null)
                .amount(tx.getAmount())
                .type(tx.getType().name())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
