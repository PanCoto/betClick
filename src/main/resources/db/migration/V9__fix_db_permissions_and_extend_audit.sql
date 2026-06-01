DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'db_procexecutor') THEN
        CREATE ROLE db_procexecutor NOLOGIN;
    END IF;

    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'app_identity') THEN
        CREATE ROLE app_identity NOLOGIN;
    END IF;

    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'administrator') THEN
        CREATE ROLE administrator NOLOGIN;
    END IF;

    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'dev_jan_kowalski') THEN
        CREATE ROLE dev_jan_kowalski NOLOGIN;
    END IF;

    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'dev_anna_nowak') THEN
        CREATE ROLE dev_anna_nowak NOLOGIN;
    END IF;

    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'dev_piotr_zielinski') THEN
        CREATE ROLE dev_piotr_zielinski NOLOGIN;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'betclick_admin') THEN
        GRANT administrator TO betclick_admin;
    END IF;

    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'betclick_runtime') THEN
        GRANT app_identity TO betclick_runtime;
        GRANT db_procexecutor TO betclick_runtime;
    END IF;

    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'betclick_employee') THEN
        GRANT app_identity TO betclick_employee;
        GRANT db_procexecutor TO betclick_employee;
    END IF;

    GRANT db_procexecutor TO app_identity;
    GRANT db_procexecutor TO dev_jan_kowalski;
    GRANT db_procexecutor TO dev_anna_nowak;
    GRANT db_procexecutor TO dev_piotr_zielinski;
END $$;

DO $$
BEGIN
    EXECUTE format(
        'GRANT CONNECT ON DATABASE %I TO betclick_runtime, betclick_employee, app_identity, dev_jan_kowalski, dev_anna_nowak, dev_piotr_zielinski',
        current_database()
    );
END $$;

GRANT USAGE ON SCHEMA public
    TO betclick_runtime, betclick_employee, app_identity,
       dev_jan_kowalski, dev_anna_nowak, dev_piotr_zielinski;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public
    TO betclick_runtime;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public
    TO app_identity;

GRANT SELECT ON ALL TABLES IN SCHEMA public
    TO betclick_employee, dev_jan_kowalski, dev_anna_nowak, dev_piotr_zielinski;

GRANT UPDATE ON public.users TO betclick_employee;
GRANT UPDATE ON public.coupons TO betclick_employee;
GRANT UPDATE ON public.events TO betclick_employee;
GRANT UPDATE ON public.markets TO betclick_employee;
GRANT UPDATE ON public.selections TO betclick_employee;
GRANT INSERT ON public.transactions TO betclick_employee;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public
    TO betclick_runtime, betclick_employee,
       dev_jan_kowalski, dev_anna_nowak, dev_piotr_zielinski;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public
    TO app_identity;

GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO db_procexecutor;
GRANT EXECUTE ON ALL PROCEDURES IN SCHEMA public TO db_procexecutor;
GRANT EXECUTE ON ALL ROUTINES IN SCHEMA public TO db_procexecutor;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO betclick_runtime;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_identity;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT ON TABLES TO betclick_employee;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT ON TABLES TO dev_jan_kowalski, dev_anna_nowak, dev_piotr_zielinski;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO betclick_runtime, betclick_employee,
       dev_jan_kowalski, dev_anna_nowak, dev_piotr_zielinski;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO app_identity;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT EXECUTE ON FUNCTIONS TO db_procexecutor;

CREATE TABLE IF NOT EXISTS public.audit_log (
    id          BIGSERIAL    PRIMARY KEY,
    table_name  VARCHAR(100) NOT NULL,
    operation   VARCHAR(10)  NOT NULL,
    db_user     VARCHAR(100) NOT NULL DEFAULT current_user,
    record_id   BIGINT,
    old_data    JSONB,
    new_data    JSONB,
    changed_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

ALTER TABLE public.audit_log
    ADD COLUMN IF NOT EXISTS table_name VARCHAR(100) NOT NULL DEFAULT 'unknown',
    ADD COLUMN IF NOT EXISTS operation VARCHAR(10) NOT NULL DEFAULT 'UNKNOWN',
    ADD COLUMN IF NOT EXISTS db_user VARCHAR(100) NOT NULL DEFAULT current_user,
    ADD COLUMN IF NOT EXISTS record_id BIGINT,
    ADD COLUMN IF NOT EXISTS old_data JSONB,
    ADD COLUMN IF NOT EXISTS new_data JSONB,
    ADD COLUMN IF NOT EXISTS changed_at TIMESTAMP NOT NULL DEFAULT NOW();

ALTER TABLE public.audit_log ALTER COLUMN table_name DROP DEFAULT;
ALTER TABLE public.audit_log ALTER COLUMN operation DROP DEFAULT;

CREATE OR REPLACE FUNCTION public.log_all_changes()
RETURNS TRIGGER AS $$
DECLARE
    v_record_id BIGINT;
BEGIN
    BEGIN
        IF TG_OP IN ('INSERT', 'UPDATE') THEN
            v_record_id := (to_jsonb(NEW) ->> 'id')::BIGINT;
        ELSE
            v_record_id := (to_jsonb(OLD) ->> 'id')::BIGINT;
        END IF;
    EXCEPTION WHEN OTHERS THEN
        v_record_id := NULL;
    END;

    INSERT INTO public.audit_log (
        table_name,
        operation,
        db_user,
        record_id,
        old_data,
        new_data
    )
    VALUES (
        TG_TABLE_NAME,
        TG_OP,
        current_user,
        v_record_id,
        CASE WHEN TG_OP IN ('UPDATE', 'DELETE') THEN to_jsonb(OLD) ELSE NULL END,
        CASE WHEN TG_OP IN ('INSERT', 'UPDATE') THEN to_jsonb(NEW) ELSE NULL END
    );

    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

DROP TRIGGER IF EXISTS trg_audit_user_balance ON public.users;
DROP TRIGGER IF EXISTS trg_audit_coupon_status ON public.coupons;

DO $$
DECLARE
    t text;
BEGIN
    FOR t IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'public'
          AND tablename NOT IN ('audit_log', 'flyway_schema_history')
    LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS audit_trigger_%I ON public.%I', t, t);

        EXECUTE format(
            'CREATE TRIGGER audit_trigger_%I
             AFTER INSERT OR UPDATE OR DELETE ON public.%I
             FOR EACH ROW EXECUTE FUNCTION public.log_all_changes()',
            t, t
        );
    END LOOP;
END $$;

REVOKE INSERT, UPDATE, DELETE ON public.audit_log FROM betclick_runtime, betclick_employee;
GRANT SELECT ON public.audit_log TO betclick_employee;
GRANT EXECUTE ON FUNCTION public.log_all_changes() TO db_procexecutor;
