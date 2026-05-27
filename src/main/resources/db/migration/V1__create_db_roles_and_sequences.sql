CREATE SEQUENCE IF NOT EXISTS betclick_ticket_seq
  START WITH 1 INCREMENT BY 1 NO MAXVALUE CACHE 50;


DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'betclick_runtime') THEN
    CREATE ROLE betclick_runtime LOGIN PASSWORD NULL;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'betclick_employee') THEN
    CREATE ROLE betclick_employee LOGIN PASSWORD NULL;
  END IF;
END $$;
