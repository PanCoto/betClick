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
        COALESCE(ABS(SUM(amount) FILTER (WHERE type = 'WITHDRAWAL')), 0) AS withdrawals,
        COALESCE(ABS(SUM(amount) FILTER (WHERE type = 'BET')), 0)    AS bets_placed,
        COALESCE(SUM(amount) FILTER (WHERE type = 'WIN'), 0)         AS wins_paid,
        COALESCE(ABS(SUM(amount) FILTER (WHERE type = 'BET')), 0)
          - COALESCE(SUM(amount) FILTER (WHERE type = 'WIN'), 0)     AS house_profit
    FROM transactions
    WHERE DATE(created_at) BETWEEN p_date_from AND p_date_to
    GROUP BY DATE(created_at)
    ORDER BY report_date;
END;
$$ LANGUAGE plpgsql STABLE;

GRANT EXECUTE ON FUNCTION get_turnover_report(DATE, DATE) TO betclick_runtime, betclick_employee;
