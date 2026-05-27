CREATE OR REPLACE FUNCTION generate_ticket_number()
RETURNS VARCHAR(25) AS $$
BEGIN
    RETURN 'BET-' || TO_CHAR(NOW(), 'YYYYMMDD') || '-'
           || LPAD(nextval('betclick_ticket_seq')::TEXT, 6, '0');
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION calculate_total_odds(p_selection_ids BIGINT[])
RETURNS DECIMAL(10,2) AS $$
DECLARE
    v_total DECIMAL(10,2);
BEGIN
    SELECT ROUND(EXP(SUM(LN(odds))), 2)
    INTO v_total
    FROM selections
    WHERE id = ANY(p_selection_ids) AND is_active = TRUE;

    IF v_total IS NULL THEN
        RAISE EXCEPTION 'Nie znaleziono aktywnych selekcji dla podanych ID';
    END IF;
    RETURN v_total;
END;
$$ LANGUAGE plpgsql STABLE;


CREATE OR REPLACE FUNCTION get_user_stats(p_user_id BIGINT)
RETURNS TABLE (
    total_bets    BIGINT,
    won_bets      BIGINT,
    lost_bets     BIGINT,
    active_bets   BIGINT,
    total_staked  DECIMAL(12,2),
    total_won     DECIMAL(12,2),
    win_rate      DECIMAL(5,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        COUNT(*)                                                         AS total_bets,
        COUNT(*) FILTER (WHERE status = 'WON')                          AS won_bets,
        COUNT(*) FILTER (WHERE status = 'LOST')                         AS lost_bets,
        COUNT(*) FILTER (WHERE status = 'ACTIVE')                       AS active_bets,
        COALESCE(SUM(stake), 0)                                          AS total_staked,
        COALESCE(SUM(actual_win), 0)                                     AS total_won,
        CASE WHEN COUNT(*) FILTER (WHERE status IN ('WON','LOST')) > 0
             THEN ROUND(
               COUNT(*) FILTER (WHERE status = 'WON')::DECIMAL /
               COUNT(*) FILTER (WHERE status IN ('WON','LOST')) * 100, 2)
             ELSE 0.00 END                                               AS win_rate
    FROM coupons
    WHERE user_id = p_user_id;
END;
$$ LANGUAGE plpgsql STABLE;


CREATE OR REPLACE FUNCTION get_top_events(p_limit INT DEFAULT 10)
RETURNS TABLE (
    event_id    BIGINT,
    event_name  VARCHAR,
    sport_name  VARCHAR,
    bet_count   BIGINT,
    total_staked DECIMAL(12,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        e.id,
        CAST(e.name AS VARCHAR),
        CAST(sp.name AS VARCHAR),
        COUNT(DISTINCT cs.coupon_id),
        COALESCE(SUM(c.stake), 0)
    FROM events e
    JOIN leagues l   ON l.id = e.league_id
    JOIN sports sp   ON sp.id = l.sport_id
    JOIN markets m   ON m.event_id = e.id
    JOIN selections s ON s.market_id = m.id
    JOIN coupon_selections cs ON cs.selection_id = s.id
    JOIN coupons c    ON c.id = cs.coupon_id
    GROUP BY e.id, e.name, sp.name
    ORDER BY COUNT(DISTINCT cs.coupon_id) DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql STABLE;


CREATE OR REPLACE FUNCTION get_turnover_report(
    p_date_from DATE,
    p_date_to   DATE
)
RETURNS TABLE (
    report_date  DATE,
    deposits     DECIMAL(12,2),
    withdrawals  DECIMAL(12,2),
    bets_placed  DECIMAL(12,2),
    wins_paid    DECIMAL(12,2),
    house_profit DECIMAL(12,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        DATE(created_at)                                      AS report_date,
        COALESCE(SUM(amount) FILTER (WHERE type = 'DEPOSIT'), 0)     AS deposits,
        COALESCE(ABS(SUM(amount)) FILTER (WHERE type = 'WITHDRAWAL'), 0) AS withdrawals,
        COALESCE(ABS(SUM(amount)) FILTER (WHERE type = 'BET'), 0)    AS bets_placed,
        COALESCE(SUM(amount) FILTER (WHERE type = 'WIN'), 0)         AS wins_paid,
        COALESCE(ABS(SUM(amount)) FILTER (WHERE type = 'BET'), 0)
          - COALESCE(SUM(amount) FILTER (WHERE type = 'WIN'), 0)     AS house_profit
    FROM transactions
    WHERE DATE(created_at) BETWEEN p_date_from AND p_date_to
    GROUP BY DATE(created_at)
    ORDER BY report_date;
END;
$$ LANGUAGE plpgsql STABLE;

GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO betclick_runtime, betclick_employee;
