CREATE OR REPLACE PROCEDURE place_bet(
    p_user_id       BIGINT,
    p_selection_ids BIGINT[],
    p_stake         DECIMAL(12,2),
    OUT o_coupon_id     BIGINT,
    OUT o_ticket_number VARCHAR(25),
    OUT o_total_odds    DECIMAL(10,2),
    OUT o_potential_win DECIMAL(12,2)
)
LANGUAGE plpgsql AS $$
DECLARE
    v_balance       DECIMAL(12,2);
    v_is_active     BOOLEAN;
    v_event_status  VARCHAR(20);
    v_sel_id        BIGINT;
    v_total_odds    DECIMAL(10,2);
    v_new_coupon_id BIGINT;
    v_ticket        VARCHAR(25);
    v_odds          DECIMAL(6,2);
BEGIN
    SELECT balance, is_active INTO v_balance, v_is_active
    FROM users WHERE id = p_user_id FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Użytkownik o ID % nie istnieje', p_user_id;
    END IF;

    IF NOT v_is_active THEN
        RAISE EXCEPTION 'Konto użytkownika jest zablokowane';
    END IF;

    IF v_balance < p_stake THEN
        RAISE EXCEPTION 'Niewystarczające środki. Wymagane: %, Dostępne: %',
            p_stake, v_balance;
    END IF;

    FOREACH v_sel_id IN ARRAY p_selection_ids LOOP
        SELECT e.status INTO v_event_status
        FROM events e
        JOIN markets m  ON m.event_id = e.id
        JOIN selections s ON s.market_id = m.id
        WHERE s.id = v_sel_id AND s.is_active = TRUE;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'Selekcja o ID % nie jest dostępna', v_sel_id;
        END IF;

        IF v_event_status != 'UPCOMING' THEN
            RAISE EXCEPTION 'Nie można obstawiać wydarzenia o statusie: %',
                v_event_status;
        END IF;
    END LOOP;

    v_total_odds := calculate_total_odds(p_selection_ids);

    v_ticket := generate_ticket_number();

    INSERT INTO coupons (user_id, ticket_number, stake, total_odds, potential_win)
    VALUES (p_user_id, v_ticket, p_stake, v_total_odds,
            ROUND(p_stake * v_total_odds, 2))
    RETURNING id INTO v_new_coupon_id;


    PERFORM set_config('betclick.enforce_event_status_check', 'true', TRUE);


    FOREACH v_sel_id IN ARRAY p_selection_ids LOOP
        SELECT odds INTO v_odds FROM selections WHERE id = v_sel_id;
        INSERT INTO coupon_selections (coupon_id, selection_id, odds_at_placement)
        VALUES (v_new_coupon_id, v_sel_id, v_odds);
    END LOOP;


    PERFORM set_config('betclick.enforce_event_status_check', 'false', TRUE);


    UPDATE users SET balance = balance - p_stake WHERE id = p_user_id;


    INSERT INTO transactions (user_id, coupon_id, amount, type, description)
    VALUES (p_user_id, v_new_coupon_id, -p_stake, 'BET',
            'Zakład na kupon ' || v_ticket);


    o_coupon_id     := v_new_coupon_id;
    o_ticket_number := v_ticket;
    o_total_odds    := v_total_odds;
    o_potential_win := ROUND(p_stake * v_total_odds, 2);

END;
$$;


CREATE OR REPLACE PROCEDURE settle_event(
    p_event_id              BIGINT,
    p_result_a              INTEGER,
    p_result_b              INTEGER,
    p_winning_selection_ids BIGINT[]
)
LANGUAGE plpgsql AS $$
DECLARE
    v_status     VARCHAR(20);
    v_coupon     RECORD;
    v_all_won    BOOLEAN;
    v_win_amount DECIMAL(12,2);
BEGIN
    SELECT status INTO v_status FROM events WHERE id = p_event_id FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Wydarzenie o ID % nie istnieje', p_event_id;
    END IF;
    IF v_status NOT IN ('UPCOMING','LIVE') THEN
        RAISE EXCEPTION 'Wydarzenie jest już zakończone lub anulowane (status: %)', v_status;
    END IF;

    UPDATE events
    SET status = 'FINISHED', result_a = p_result_a, result_b = p_result_b,
        updated_at = NOW()
    WHERE id = p_event_id;

    UPDATE selections
    SET is_winner = (id = ANY(p_winning_selection_ids))
    WHERE market_id IN (SELECT id FROM markets WHERE event_id = p_event_id);

    UPDATE markets SET is_settled = TRUE WHERE event_id = p_event_id;

    FOR v_coupon IN
        SELECT DISTINCT c.id, c.user_id, c.stake, c.total_odds, c.potential_win
        FROM coupons c
        JOIN coupon_selections cs ON cs.coupon_id = c.id
        JOIN selections s ON s.id = cs.selection_id
        JOIN markets m ON m.id = s.market_id
        WHERE m.event_id = p_event_id AND c.status = 'ACTIVE'
    LOOP
        SELECT BOOL_AND(COALESCE(s.is_winner, FALSE)) INTO v_all_won
        FROM coupon_selections cs
        JOIN selections s ON s.id = cs.selection_id
        WHERE cs.coupon_id = v_coupon.id;

        IF v_all_won THEN
            v_win_amount := v_coupon.potential_win;
            UPDATE coupons
            SET status = 'WON', actual_win = v_win_amount, settled_at = NOW()
            WHERE id = v_coupon.id;

            UPDATE users SET balance = balance + v_win_amount
            WHERE id = v_coupon.user_id;

            INSERT INTO transactions (user_id, coupon_id, amount, type, description)
            VALUES (v_coupon.user_id, v_coupon.id, v_win_amount, 'WIN',
                    'Wygrana z kuponu ' || v_coupon.id);
        ELSE
            UPDATE coupons
            SET status = 'LOST', actual_win = 0.00, settled_at = NOW()
            WHERE id = v_coupon.id;
        END IF;
    END LOOP;
END;
$$;


CREATE OR REPLACE PROCEDURE deposit_funds(
    p_user_id BIGINT,
    p_amount  DECIMAL(12,2),
    OUT o_new_balance DECIMAL(12,2)
)
LANGUAGE plpgsql AS $$
BEGIN
    IF p_amount <= 0 THEN
        RAISE EXCEPTION 'Kwota wpłaty musi być większa od 0';
    END IF;
    IF p_amount > 50000 THEN
        RAISE EXCEPTION 'Maksymalna jednorazowa wpłata to 50000 PLN';
    END IF;

    UPDATE users SET balance = balance + p_amount
    WHERE id = p_user_id AND is_active = TRUE
    RETURNING balance INTO o_new_balance;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Użytkownik o ID % nie istnieje lub jest zablokowany', p_user_id;
    END IF;

    INSERT INTO transactions (user_id, amount, type, description)
    VALUES (p_user_id, p_amount, 'DEPOSIT', 'Doładowanie konta');
END;
$$;


CREATE OR REPLACE PROCEDURE withdraw_funds(
    p_user_id BIGINT,
    p_amount  DECIMAL(12,2),
    OUT o_new_balance DECIMAL(12,2)
)
LANGUAGE plpgsql AS $$
DECLARE
    v_balance DECIMAL(12,2);
BEGIN
    IF p_amount <= 0 THEN
        RAISE EXCEPTION 'Kwota wypłaty musi być większa od 0';
    END IF;

    SELECT balance INTO v_balance
    FROM users WHERE id = p_user_id AND is_active = TRUE FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Użytkownik o ID % nie istnieje lub jest zablokowany', p_user_id;
    END IF;

    IF v_balance < p_amount THEN
        RAISE EXCEPTION 'Niewystarczające środki. Dostępne: %, Żądane: %',
            v_balance, p_amount;
    END IF;

    UPDATE users SET balance = balance - p_amount WHERE id = p_user_id
    RETURNING balance INTO o_new_balance;

    INSERT INTO transactions (user_id, amount, type, description)
    VALUES (p_user_id, -p_amount, 'WITHDRAWAL', 'Wypłata środków');
END;
$$;

GRANT EXECUTE ON ALL PROCEDURES IN SCHEMA public TO betclick_runtime, betclick_employee;
GRANT EXECUTE ON ALL ROUTINES IN SCHEMA public TO betclick_runtime, betclick_employee;