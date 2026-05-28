CREATE OR REPLACE FUNCTION fn_trg_update_event_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_event_timestamp
    BEFORE UPDATE ON events
    FOR EACH ROW EXECUTE FUNCTION fn_trg_update_event_timestamp();


CREATE OR REPLACE FUNCTION fn_trg_prevent_settled_selection_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.is_winner IS NOT NULL AND NEW.is_winner IS DISTINCT FROM OLD.is_winner THEN
        RAISE EXCEPTION
            'Nie można zmienić wyniku już rozliczonej selekcji (id=%).', OLD.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_settled_selection_change
    BEFORE UPDATE OF is_winner ON selections
    FOR EACH ROW EXECUTE FUNCTION fn_trg_prevent_settled_selection_change();


CREATE OR REPLACE FUNCTION fn_trg_audit_user_balance()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.balance IS DISTINCT FROM NEW.balance THEN
        INSERT INTO audit_log (table_name, operation, db_user, record_id, old_data, new_data)
        VALUES (
            'users', 'UPDATE', current_user, NEW.id,
            jsonb_build_object('balance', OLD.balance, 'is_active', OLD.is_active),
            jsonb_build_object('balance', NEW.balance, 'is_active', NEW.is_active)
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER trg_audit_user_balance
    AFTER UPDATE OF balance ON users
    FOR EACH ROW EXECUTE FUNCTION fn_trg_audit_user_balance();


CREATE OR REPLACE FUNCTION fn_trg_audit_coupon_status()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO audit_log (table_name, operation, db_user, record_id, old_data, new_data)
        VALUES (
            'coupons', 'UPDATE', current_user, NEW.id,
            jsonb_build_object('status', OLD.status),
            jsonb_build_object('status', NEW.status, 'actual_win', NEW.actual_win,
                               'settled_at', NEW.settled_at)
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER trg_audit_coupon_status
    AFTER UPDATE OF status ON coupons
    FOR EACH ROW EXECUTE FUNCTION fn_trg_audit_coupon_status();


CREATE OR REPLACE FUNCTION fn_trg_check_event_status_on_bet()
RETURNS TRIGGER AS $$
DECLARE
    v_status  VARCHAR(20);
    v_enforce BOOLEAN;
BEGIN

    BEGIN
        v_enforce := current_setting('betclick.enforce_event_status_check')::BOOLEAN;
    EXCEPTION WHEN OTHERS THEN

        v_enforce := FALSE;
    END;

    IF NOT v_enforce THEN
        RETURN NEW;
    END IF;

    SELECT e.status INTO v_status
    FROM events e
    JOIN markets m  ON m.event_id = e.id
    JOIN selections s ON s.market_id = m.id
    WHERE s.id = NEW.selection_id;

    IF v_status IS DISTINCT FROM 'UPCOMING' THEN
        RAISE EXCEPTION
            'Nie można dodać selekcji do kuponu — event nie jest w statusie UPCOMING (status: %).', v_status;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_event_status_on_bet
    BEFORE INSERT ON coupon_selections
    FOR EACH ROW EXECUTE FUNCTION fn_trg_check_event_status_on_bet();

GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO betclick_runtime, betclick_employee;