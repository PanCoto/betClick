CREATE OR REPLACE PROCEDURE settle_event_with_cursor(
    p_event_id BIGINT,
    p_result_a INTEGER,
    p_result_b INTEGER,
    p_winning_selection_ids BIGINT[]
)
LANGUAGE plpgsql AS $$
DECLARE
    v_coupon_cursor CURSOR FOR
        SELECT DISTINCT c.id, c.user_id, c.stake, c.total_odds, c.potential_win
        FROM coupons c
        JOIN coupon_selections cs ON cs.coupon_id = c.id
        JOIN selections s ON s.id = cs.selection_id
        JOIN markets m ON m.id = s.market_id
        WHERE m.event_id = p_event_id AND c.status = 'ACTIVE';
    v_coupon RECORD;
    v_status VARCHAR(20);
    v_all_settled BOOLEAN;
    v_any_lost BOOLEAN;
    v_all_won BOOLEAN;
    v_win_amount DECIMAL(12,2);
BEGIN
    SELECT status INTO v_status
    FROM events
    WHERE id = p_event_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Wydarzenie o ID % nie istnieje', p_event_id;
    END IF;

    IF v_status NOT IN ('UPCOMING','LIVE') THEN
        RAISE EXCEPTION 'Wydarzenie jest juz zakonczone lub anulowane (status: %)', v_status;
    END IF;

    UPDATE events
    SET status = 'FINISHED', result_a = p_result_a, result_b = p_result_b,
        updated_at = NOW()
    WHERE id = p_event_id;

    UPDATE selections
    SET is_winner = (id = ANY(p_winning_selection_ids))
    WHERE market_id IN (SELECT id FROM markets WHERE event_id = p_event_id);

    UPDATE markets SET is_settled = TRUE WHERE event_id = p_event_id;

    OPEN v_coupon_cursor;
    LOOP
        FETCH v_coupon_cursor INTO v_coupon;
        EXIT WHEN NOT FOUND;

        SELECT
            BOOL_AND(m.is_settled),
            BOOL_OR(m.is_settled AND COALESCE(s.is_winner, FALSE) = FALSE),
            BOOL_AND(m.is_settled AND COALESCE(s.is_winner, FALSE))
        INTO v_all_settled, v_any_lost, v_all_won
        FROM coupon_selections cs
        JOIN selections s ON s.id = cs.selection_id
        JOIN markets m ON m.id = s.market_id
        WHERE cs.coupon_id = v_coupon.id;

        IF COALESCE(v_any_lost, FALSE) THEN
            UPDATE coupons
            SET status = 'LOST', actual_win = 0.00, settled_at = NOW()
            WHERE id = v_coupon.id;
        ELSIF COALESCE(v_all_settled, FALSE) AND COALESCE(v_all_won, FALSE) THEN
            v_win_amount := v_coupon.potential_win;
            UPDATE coupons
            SET status = 'WON', actual_win = v_win_amount, settled_at = NOW()
            WHERE id = v_coupon.id;
            UPDATE users SET balance = balance + v_win_amount WHERE id = v_coupon.user_id;
            INSERT INTO transactions (user_id, coupon_id, amount, type, description)
            VALUES (v_coupon.user_id, v_coupon.id, v_win_amount, 'WIN',
                    'Wygrana z kuponu (kursorem)');
        END IF;
    END LOOP;
    CLOSE v_coupon_cursor;
END;
$$;

GRANT EXECUTE ON PROCEDURE settle_event_with_cursor TO betclick_runtime, betclick_employee;
