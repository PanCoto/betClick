INSERT INTO sports (name, category) VALUES
  ('Piłka Nożna',  'Sporty Drużynowe'),
  ('Tenis',        'Sporty Indywidualne'),
  ('Koszykówka',   'Sporty Drużynowe'),
  ('Siatkówka',    'Sporty Drużynowe'),
  ('Hokej',        'Sporty Drużynowe'),
  ('Boks',         'Sporty Walki'),
  ('MMA',          'Sporty Walki'),
  ('Żużel',        'Sporty Motorowe')
ON CONFLICT (name) DO NOTHING;


INSERT INTO users (login, password_hash, email, first_name, last_name,
                   date_of_birth, balance, role)
VALUES ('admin', '$2a$12$Ut/9KGR6LYEsKVVQofhoduWfV/5Y3uxFzZmY7GhtmbR0Mbka2UajG',
        'admin@betclick.pl', 'Administrator', 'Systemu',
        '1990-01-01', 0.00, 'ADMIN')
ON CONFLICT (login) DO NOTHING;

INSERT INTO leagues (sport_id, name, country, season)
SELECT s.id, 'PKO BP Ekstraklasa', 'Polska', '2024/25'
FROM sports s WHERE s.name = 'Piłka Nożna'
ON CONFLICT DO NOTHING;

INSERT INTO leagues (sport_id, name, country, season)
SELECT s.id, 'Premier League', 'Anglia', '2024/25'
FROM sports s WHERE s.name = 'Piłka Nożna'
ON CONFLICT DO NOTHING;

INSERT INTO leagues (sport_id, name, country, season)
SELECT s.id, 'La Liga', 'Hiszpania', '2024/25'
FROM sports s WHERE s.name = 'Piłka Nożna'
ON CONFLICT DO NOTHING;

INSERT INTO leagues (sport_id, name, country, season)
SELECT s.id, 'NBA', 'USA', '2024/25'
FROM sports s WHERE s.name = 'Koszykówka'
ON CONFLICT DO NOTHING;
