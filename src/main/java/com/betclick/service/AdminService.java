package com.betclick.service;

import com.betclick.dto.request.SettleEventRequest;
import com.betclick.dto.response.TurnoverReportRow;
import com.betclick.model.AuditLog;
import com.betclick.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    private static final int AUDIT_LOG_LIMIT = 50;

    private final AuditLogRepository auditLogRepository;
    private final JdbcTemplate jdbcTemplate;

    public AdminService(AuditLogRepository auditLogRepository, JdbcTemplate jdbcTemplate) {
        this.auditLogRepository = auditLogRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void settleEvent(Long eventId, SettleEventRequest request) {
        Long[] winningSelIds = request.getWinningSelectionIds().toArray(new Long[0]);

        try {
            jdbcTemplate.execute((Connection conn) -> {
                try (CallableStatement call = conn.prepareCall("CALL settle_event_with_cursor(?, ?, ?, ?)")) {
                    call.setLong(1, eventId);
                    call.setInt(2, request.getResultA());
                    call.setInt(3, request.getResultB());
                    call.setArray(4, conn.createArrayOf("BIGINT", winningSelIds));

                    call.execute();
                    return null;
                }
            });
        } catch (Exception e) {
            log.warn("Could not settle event {}", eventId, e);
            throw new IllegalArgumentException("Rozliczenie wydarzenia nie powiodlo sie. Sprobuj ponownie pozniej.");
        }
    }

    @Transactional(readOnly = true)
    public List<TurnoverReportRow> getTurnoverReport(LocalDate from, LocalDate to) {
        String sql = "SELECT report_date, deposits, withdrawals, bets_placed, wins_paid, house_profit " +
                     "FROM get_turnover_report(?, ?)";
        
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> TurnoverReportRow.builder()
                    .reportDate(rs.getDate("report_date").toLocalDate())
                    .deposits(rs.getBigDecimal("deposits"))
                    .withdrawals(rs.getBigDecimal("withdrawals"))
                    .betsPlaced(rs.getBigDecimal("bets_placed"))
                    .winsPaid(rs.getBigDecimal("wins_paid"))
                    .houseProfit(rs.getBigDecimal("house_profit"))
                    .build(),
                    java.sql.Date.valueOf(from),
                    java.sql.Date.valueOf(to)
            );
        } catch (Exception e) {
            log.warn("Could not generate turnover report from {} to {}", from, to, e);
            throw new RuntimeException("Nie udalo sie wygenerowac raportu obrotu.", e);
        }
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogs() {
        return auditLogRepository.findAllByOrderByChangedAtDesc(PageRequest.of(0, AUDIT_LOG_LIMIT));
    }
}
