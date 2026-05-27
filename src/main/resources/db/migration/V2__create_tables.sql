CREATE TABLE IF NOT EXISTS event_statuses (
    id   BIGSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);


CREATE TABLE IF NOT EXISTS coupon_statuses (
    id   BIGSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);


CREATE TABLE IF NOT EXISTS bet_statuses (
    id   BIGSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);


CREATE TABLE IF NOT EXISTS transaction_statuses (
    id   BIGSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);


CREATE TABLE IF NOT EXISTS notification_statuses (
    id   BIGSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);


CREATE TABLE IF NOT EXISTS notification_types (
    id   BIGSERIAL PRIMARY KEY,
    code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL
);


CREATE TABLE IF NOT EXISTS bonus_statuses (
    id   BIGSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);


CREATE TABLE IF NOT EXISTS bonus_types (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(60) NOT NULL UNIQUE,
    name        VARCHAR(120) NOT NULL,
    description TEXT
);


CREATE TABLE IF NOT EXISTS user_levels (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(40) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    min_total_won   DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    min_total_stake DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    sort_order      INTEGER NOT NULL UNIQUE
);


CREATE TABLE IF NOT EXISTS game_types (
    id   BIGSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);


CREATE TABLE IF NOT EXISTS game_round_statuses (
    id   BIGSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);


CREATE TABLE IF NOT EXISTS cashout_statuses (
    id   BIGSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);


CREATE TABLE IF NOT EXISTS favorite_statuses (
    id   BIGSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);


CREATE TABLE IF NOT EXISTS users (
    id               BIGSERIAL       PRIMARY KEY,
    login            VARCHAR(50)     NOT NULL,
    password_hash    VARCHAR(255)    NOT NULL,
    email            VARCHAR(100)    NOT NULL,
    first_name       VARCHAR(50)     NOT NULL,
    last_name        VARCHAR(50)     NOT NULL,
    date_of_birth    DATE            NOT NULL,
    phone_number     VARCHAR(20),
    registration_date TIMESTAMP      NOT NULL DEFAULT NOW(),
    balance          DECIMAL(12,2)   NOT NULL DEFAULT 0.00,
    is_active        BOOLEAN         NOT NULL DEFAULT TRUE,
    role             VARCHAR(20)     NOT NULL DEFAULT 'PLAYER',
    level_id         BIGINT,
    CONSTRAINT uq_users_login   UNIQUE (login),
    CONSTRAINT uq_users_email   UNIQUE (email),
    CONSTRAINT chk_balance      CHECK (balance >= 0),
    CONSTRAINT chk_user_role    CHECK (role IN ('PLAYER','ADMIN','MODERATOR','GUEST')),
    CONSTRAINT fk_users_level_id FOREIGN KEY (level_id) REFERENCES user_levels(id)
);


CREATE TABLE IF NOT EXISTS sports (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    category    VARCHAR(100),
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_sports_name UNIQUE (name)
);


CREATE TABLE IF NOT EXISTS leagues (
    id        BIGSERIAL    PRIMARY KEY,
    sport_id  BIGINT       NOT NULL REFERENCES sports(id) ON DELETE RESTRICT,
    name      VARCHAR(200) NOT NULL,
    country   VARCHAR(100),
    season    VARCHAR(20),
    is_active BOOLEAN      NOT NULL DEFAULT TRUE
);


CREATE TABLE IF NOT EXISTS events (
    id          BIGSERIAL    PRIMARY KEY,
    league_id   BIGINT       NOT NULL REFERENCES leagues(id) ON DELETE RESTRICT,
    name        VARCHAR(300) NOT NULL,
    team_a      VARCHAR(200) NOT NULL,
    team_b      VARCHAR(200) NOT NULL,
    start_time  TIMESTAMP    NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'UPCOMING',
    result_a    INTEGER,
    result_b    INTEGER,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_event_status CHECK (status IN ('UPCOMING','LIVE','FINISHED','CANCELLED')),
    CONSTRAINT chk_result       CHECK (
        (status IN ('UPCOMING','LIVE','CANCELLED') AND result_a IS NULL AND result_b IS NULL)
        OR (status = 'FINISHED' AND result_a IS NOT NULL AND result_b IS NOT NULL)
    )
);


CREATE TABLE IF NOT EXISTS markets (
    id          BIGSERIAL    PRIMARY KEY,
    event_id    BIGINT       NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    is_settled  BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
);


CREATE TABLE IF NOT EXISTS selections (
    id         BIGSERIAL     PRIMARY KEY,
    market_id  BIGINT        NOT NULL REFERENCES markets(id) ON DELETE CASCADE,
    name       VARCHAR(200)  NOT NULL,
    odds       DECIMAL(6,2)  NOT NULL,
    is_winner  BOOLEAN,
    is_active  BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_odds CHECK (odds >= 1.00)
);


CREATE TABLE IF NOT EXISTS coupons (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    ticket_number   VARCHAR(25)     NOT NULL,
    stake           DECIMAL(12,2)   NOT NULL,
    total_odds      DECIMAL(10,2)   NOT NULL,
    potential_win   DECIMAL(12,2)   NOT NULL,
    actual_win      DECIMAL(12,2)   NOT NULL DEFAULT 0.00,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    placed_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    settled_at      TIMESTAMP,
    CONSTRAINT uq_ticket_number   UNIQUE (ticket_number),
    CONSTRAINT chk_stake          CHECK (stake > 0),
    CONSTRAINT chk_total_odds     CHECK (total_odds >= 1.00),
    CONSTRAINT chk_coupon_status  CHECK (status IN ('ACTIVE','WON','LOST','CANCELLED','CASHED_OUT'))
);


CREATE TABLE IF NOT EXISTS coupon_selections (
    id                  BIGSERIAL    PRIMARY KEY,
    coupon_id           BIGINT       NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
    selection_id        BIGINT       NOT NULL REFERENCES selections(id) ON DELETE RESTRICT,
    odds_at_placement   DECIMAL(6,2) NOT NULL,
    CONSTRAINT uq_coupon_selection UNIQUE (coupon_id, selection_id)
);


CREATE TABLE IF NOT EXISTS transactions (
    id          BIGSERIAL      PRIMARY KEY,
    user_id     BIGINT         NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    coupon_id   BIGINT         REFERENCES coupons(id) ON DELETE SET NULL,
    amount      DECIMAL(12,2)  NOT NULL,
    type        VARCHAR(20)    NOT NULL,
    description TEXT,
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_transaction_type CHECK (
        type IN ('DEPOSIT','WITHDRAWAL','BET','WIN','CASHOUT','REFUND','BONUS','GAME_BET','GAME_WIN')
    )
);


CREATE TABLE IF NOT EXISTS cashouts (
    id              BIGSERIAL PRIMARY KEY,
    coupon_id       BIGINT NOT NULL REFERENCES coupons(id) ON DELETE RESTRICT,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    original_stake  DECIMAL(12,2) NOT NULL,
    potential_win   DECIMAL(12,2) NOT NULL,
    cashout_amount  DECIMAL(12,2) NOT NULL,
    status_id       BIGINT NOT NULL REFERENCES cashout_statuses(id),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    accepted_at     TIMESTAMP,
    CONSTRAINT uq_cashouts_coupon UNIQUE (coupon_id),
    CONSTRAINT chk_cashout_amount CHECK (cashout_amount > 0)
);


CREATE TABLE IF NOT EXISTS favorite_events (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_id   BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    status_id  BIGINT NOT NULL REFERENCES favorite_statuses(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_favorite_events_user_event UNIQUE (user_id, event_id)
);


CREATE TABLE IF NOT EXISTS notifications (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type_id             BIGINT NOT NULL REFERENCES notification_types(id),
    status_id           BIGINT NOT NULL REFERENCES notification_statuses(id),
    title               VARCHAR(160) NOT NULL,
    message             TEXT NOT NULL,
    related_entity_type VARCHAR(60),
    related_entity_id   BIGINT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    read_at             TIMESTAMP
);


CREATE TABLE IF NOT EXISTS bonuses (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bonus_type_id    BIGINT NOT NULL REFERENCES bonus_types(id),
    status_id        BIGINT NOT NULL REFERENCES bonus_statuses(id),
    amount           DECIMAL(12,2) NOT NULL,
    remaining_amount DECIMAL(12,2) NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    used_at          TIMESTAMP,
    expires_at       TIMESTAMP,
    CONSTRAINT chk_bonus_amount CHECK (amount > 0 AND remaining_amount >= 0),
    CONSTRAINT uq_bonus_user_type UNIQUE (user_id, bonus_type_id)
);


CREATE TABLE IF NOT EXISTS war_game_rounds (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    game_type_id     BIGINT REFERENCES game_types(id),
    stake            DECIMAL(12,2) NOT NULL,
    selected_outcome VARCHAR(40) NOT NULL,
    actual_outcome   VARCHAR(40) NOT NULL,
    odds             DECIMAL(6,2) NOT NULL,
    payout           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    status_id        BIGINT NOT NULL REFERENCES game_round_statuses(id),
    player_card      INTEGER NOT NULL,
    dealer_card      INTEGER NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    settled_at       TIMESTAMP,
    CONSTRAINT chk_war_stake CHECK (stake > 0),
    CONSTRAINT chk_war_odds CHECK (odds >= 1.00),
    CONSTRAINT chk_war_cards CHECK (player_card BETWEEN 2 AND 14 AND dealer_card BETWEEN 2 AND 14),
    CONSTRAINT chk_war_outcomes CHECK (
        selected_outcome IN ('PLAYER_WIN','DEALER_WIN','WAR')
        AND actual_outcome IN ('PLAYER_WIN','DEALER_WIN','WAR')
    )
);


CREATE TABLE IF NOT EXISTS player_rankings (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    total_staked       DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_won          DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_profit       DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    won_coupons_count  BIGINT NOT NULL DEFAULT 0,
    lost_coupons_count BIGINT NOT NULL DEFAULT 0,
    games_won_count    BIGINT NOT NULL DEFAULT 0,
    games_lost_count   BIGINT NOT NULL DEFAULT 0,
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_player_rankings_user UNIQUE (user_id)
);


CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGSERIAL    PRIMARY KEY,
    table_name  VARCHAR(100) NOT NULL,
    operation   VARCHAR(10)  NOT NULL,
    db_user     VARCHAR(100) NOT NULL DEFAULT current_user,
    record_id   BIGINT,
    old_data    JSONB,
    new_data    JSONB,
    changed_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);


INSERT INTO event_statuses (code, name) VALUES
    ('SCHEDULED', 'Scheduled'),
    ('LIVE', 'Live'),
    ('FINISHED', 'Finished'),
    ('SETTLED', 'Settled'),
    ('CANCELLED', 'Cancelled')
ON CONFLICT (code) DO NOTHING;

INSERT INTO coupon_statuses (code, name) VALUES
    ('ACTIVE', 'Active'),
    ('WON', 'Won'),
    ('LOST', 'Lost'),
    ('CANCELLED', 'Cancelled'),
    ('CASHED_OUT', 'Cashed out')
ON CONFLICT (code) DO NOTHING;

INSERT INTO bet_statuses (code, name) VALUES
    ('ACTIVE', 'Active'),
    ('WON', 'Won'),
    ('LOST', 'Lost'),
    ('CANCELLED', 'Cancelled')
ON CONFLICT (code) DO NOTHING;

INSERT INTO transaction_statuses (code, name) VALUES
    ('PENDING', 'Pending'),
    ('COMPLETED', 'Completed'),
    ('FAILED', 'Failed'),
    ('CANCELLED', 'Cancelled'),
    ('REFUNDED', 'Refunded')
ON CONFLICT (code) DO NOTHING;

INSERT INTO notification_statuses (code, name) VALUES
    ('UNREAD', 'Unread'),
    ('READ', 'Read')
ON CONFLICT (code) DO NOTHING;

INSERT INTO notification_types (code, name) VALUES
    ('COUPON_SETTLED', 'Coupon settled'),
    ('CASHOUT_ACCEPTED', 'Cashout accepted'),
    ('BONUS_GRANTED', 'Bonus granted'),
    ('BONUS_USED', 'Bonus used'),
    ('LEVEL_CHANGED', 'Level changed'),
    ('FAVORITE_EVENT_STARTED', 'Favorite event started'),
    ('GAME_RESULT', 'Game result'),
    ('COUPON_CANCELLED', 'Coupon cancelled')
ON CONFLICT (code) DO NOTHING;

INSERT INTO bonus_statuses (code, name) VALUES
    ('AVAILABLE', 'Available'),
    ('USED', 'Used'),
    ('EXPIRED', 'Expired'),
    ('CANCELLED', 'Cancelled')
ON CONFLICT (code) DO NOTHING;

INSERT INTO bonus_types (code, name, description) VALUES
    ('WELCOME_BONUS', 'Welcome bonus', 'Welcome bonus for a new player'),
    ('FREEBET', 'Freebet', 'Free bet bonus')
ON CONFLICT (code) DO NOTHING;

INSERT INTO user_levels (code, name, min_total_won, min_total_stake, sort_order) VALUES
    ('BRONZE', 'Bronze', 0.00, 0.00, 1),
    ('SILVER', 'Silver', 0.00, 1000.00, 2),
    ('GOLD', 'Gold', 0.00, 5000.00, 3),
    ('VIP', 'VIP', 0.00, 20000.00, 4),
    ('ELITE', 'Elite', 0.00, 50000.00, 5)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    min_total_won = EXCLUDED.min_total_won,
    min_total_stake = EXCLUDED.min_total_stake,
    sort_order = EXCLUDED.sort_order;

INSERT INTO game_types (code, name) VALUES
    ('WAR', 'War')
ON CONFLICT (code) DO NOTHING;

INSERT INTO game_round_statuses (code, name) VALUES
    ('ACTIVE', 'Active'),
    ('WON', 'Won'),
    ('LOST', 'Lost'),
    ('CANCELLED', 'Cancelled')
ON CONFLICT (code) DO NOTHING;

INSERT INTO cashout_statuses (code, name) VALUES
    ('AVAILABLE', 'Available'),
    ('ACCEPTED', 'Accepted'),
    ('REJECTED', 'Rejected'),
    ('EXPIRED', 'Expired')
ON CONFLICT (code) DO NOTHING;

INSERT INTO favorite_statuses (code, name) VALUES
    ('ACTIVE', 'Active'),
    ('REMOVED', 'Removed')
ON CONFLICT (code) DO NOTHING;


CREATE INDEX IF NOT EXISTS idx_cashouts_user_created ON cashouts(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_favorite_events_user_status ON favorite_events(user_id, status_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_status_created ON notifications(user_id, status_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_bonuses_user_status ON bonuses(user_id, status_id);
CREATE INDEX IF NOT EXISTS idx_war_game_rounds_user_created ON war_game_rounds(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_player_rankings_total_won ON player_rankings(total_won DESC, total_profit DESC);


GRANT SELECT, INSERT, UPDATE ON
    users, coupons, coupon_selections, transactions,
    sports, leagues, events, markets, selections,
    event_statuses, coupon_statuses, bet_statuses, transaction_statuses,
    notification_statuses, notification_types, bonus_statuses, bonus_types,
    user_levels, game_types, game_round_statuses, cashout_statuses,
    favorite_statuses, cashouts, favorite_events, notifications, bonuses,
    war_game_rounds, player_rankings
    TO betclick_runtime;

GRANT SELECT ON audit_log TO betclick_runtime;


GRANT SELECT, UPDATE ON coupons, events TO betclick_employee;
GRANT SELECT, INSERT, UPDATE ON sports, leagues, markets, selections TO betclick_employee;
GRANT SELECT ON
    event_statuses, coupon_statuses, bet_statuses, transaction_statuses,
    notification_statuses, notification_types, bonus_statuses, bonus_types,
    user_levels, game_types, game_round_statuses, cashout_statuses,
    favorite_statuses, cashouts, favorite_events, notifications, bonuses,
    war_game_rounds, player_rankings
    TO betclick_employee;


GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public
    TO betclick_runtime;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public
    TO betclick_employee;
