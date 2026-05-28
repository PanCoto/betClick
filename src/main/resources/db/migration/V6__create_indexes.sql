CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email  ON users (email);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_login  ON users (login);
CREATE        INDEX IF NOT EXISTS idx_users_active_role ON users (is_active, role);

CREATE INDEX IF NOT EXISTS idx_events_status       ON events (status);
CREATE INDEX IF NOT EXISTS idx_events_start_time   ON events (start_time);
CREATE INDEX IF NOT EXISTS idx_events_status_start ON events (status, start_time ASC);
CREATE INDEX IF NOT EXISTS idx_events_league_id    ON events (league_id);


CREATE INDEX IF NOT EXISTS idx_markets_event_active    ON markets (event_id, is_active);
CREATE INDEX IF NOT EXISTS idx_selections_market_active ON selections (market_id, is_active);

CREATE INDEX IF NOT EXISTS idx_coupons_user_id     ON coupons (user_id);
CREATE INDEX IF NOT EXISTS idx_coupons_status      ON coupons (status);
CREATE INDEX IF NOT EXISTS idx_coupons_user_status ON coupons (user_id, status);
CREATE INDEX IF NOT EXISTS idx_coupons_placed_at   ON coupons (placed_at DESC);

CREATE INDEX IF NOT EXISTS idx_coupon_sel_coupon    ON coupon_selections (coupon_id);
CREATE INDEX IF NOT EXISTS idx_coupon_sel_selection ON coupon_selections (selection_id);


CREATE INDEX IF NOT EXISTS idx_transactions_user_id   ON transactions (user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_user_date ON transactions (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transactions_type      ON transactions (type);

CREATE INDEX IF NOT EXISTS idx_audit_table_date ON audit_log (table_name, changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_record_id  ON audit_log (record_id);


CREATE INDEX IF NOT EXISTS idx_leagues_sport_id ON leagues (sport_id);
