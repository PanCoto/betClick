package com.betclick.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DictionaryService {

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, Long> idCache = new ConcurrentHashMap<>();

    public DictionaryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long id(String tableName, String code) {
        String key = tableName + ":" + code;
        return idCache.computeIfAbsent(key, ignored -> jdbcTemplate.queryForObject(
                "SELECT id FROM " + tableName + " WHERE code = ?",
                Long.class,
                code
        ));
    }

    public String code(String tableName, Long id) {
        if (id == null) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT code FROM " + tableName + " WHERE id = ?",
                    String.class,
                    id
            );
        } catch (Exception e) {
            return null;
        }
    }

    public String userLevelCode(Long levelId) {
        String code = code("user_levels", levelId);
        return code != null ? code : "BRONZE";
    }

    public String userLevelName(Long levelId) {
        if (levelId == null) {
            return "Bronze";
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT name FROM user_levels WHERE id = ?",
                    String.class,
                    levelId
            );
        } catch (Exception e) {
            return "Bronze";
        }
    }

    public BigDecimal availableBonusAmount(Long userId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(b.remaining_amount), 0.00) " +
                            "FROM bonuses b JOIN bonus_statuses bs ON bs.id = b.status_id " +
                            "WHERE b.user_id = ? AND bs.code = 'AVAILABLE'",
                    BigDecimal.class,
                    userId
            );
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
