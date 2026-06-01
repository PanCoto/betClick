set -euo pipefail

: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${BETCLICK_RUNTIME_PASSWORD:?BETCLICK_RUNTIME_PASSWORD is required}"
: "${BETCLICK_EMPLOYEE_PASSWORD:?BETCLICK_EMPLOYEE_PASSWORD is required}"

echo "[betClick Init] Creating database roles..."

psql -v ON_ERROR_STOP=1 \
     --username "$POSTGRES_USER" \
     --dbname "$POSTGRES_DB" \
     -v admin_role="$POSTGRES_USER" \
     -v db_name="$POSTGRES_DB" \
     -v runtime_password="$BETCLICK_RUNTIME_PASSWORD" \
     -v employee_password="$BETCLICK_EMPLOYEE_PASSWORD" \
     -v dev_jan_password="${DEV_JAN_KOWALSKI_PASSWORD:-}" \
     -v dev_anna_password="${DEV_ANNA_NOWAK_PASSWORD:-}" \
     -v dev_piotr_password="${DEV_PIOTR_ZIELINSKI_PASSWORD:-}" \
     <<-'EOSQL'

SELECT 'CREATE ROLE app_identity NOLOGIN'
WHERE NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'app_identity')\gexec

SELECT 'CREATE ROLE administrator NOLOGIN'
WHERE NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'administrator')\gexec

SELECT format('CREATE ROLE betclick_runtime WITH LOGIN PASSWORD %L CONNECTION LIMIT 20', :'runtime_password')
WHERE NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'betclick_runtime')\gexec

ALTER ROLE betclick_runtime WITH PASSWORD :'runtime_password' CONNECTION LIMIT 20;

SELECT format('CREATE ROLE betclick_employee WITH LOGIN PASSWORD %L CONNECTION LIMIT 5', :'employee_password')
WHERE NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'betclick_employee')\gexec

ALTER ROLE betclick_employee WITH PASSWORD :'employee_password' CONNECTION LIMIT 5;

SELECT 'CREATE ROLE dev_jan_kowalski LOGIN NOINHERIT CONNECTION LIMIT 3'
WHERE NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'dev_jan_kowalski')\gexec

SELECT format('ALTER ROLE dev_jan_kowalski WITH PASSWORD %L', :'dev_jan_password')
WHERE :'dev_jan_password' <> ''\gexec

SELECT 'CREATE ROLE dev_anna_nowak LOGIN NOINHERIT CONNECTION LIMIT 3'
WHERE NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'dev_anna_nowak')\gexec

SELECT format('ALTER ROLE dev_anna_nowak WITH PASSWORD %L', :'dev_anna_password')
WHERE :'dev_anna_password' <> ''\gexec

SELECT 'CREATE ROLE dev_piotr_zielinski LOGIN NOINHERIT CONNECTION LIMIT 3'
WHERE NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'dev_piotr_zielinski')\gexec

SELECT format('ALTER ROLE dev_piotr_zielinski WITH PASSWORD %L', :'dev_piotr_password')
WHERE :'dev_piotr_password' <> ''\gexec

GRANT administrator TO :"admin_role";
GRANT app_identity TO betclick_runtime;
GRANT app_identity TO betclick_employee;

GRANT CONNECT ON DATABASE :"db_name"
    TO betclick_runtime, betclick_employee,
       dev_jan_kowalski, dev_anna_nowak, dev_piotr_zielinski;
GRANT USAGE ON SCHEMA public
    TO betclick_runtime, betclick_employee,
       dev_jan_kowalski, dev_anna_nowak, dev_piotr_zielinski;

EOSQL

echo "[betClick Init] Database roles are ready."
