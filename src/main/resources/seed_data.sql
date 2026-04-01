-- ============================================================
-- BookMyShow Seed Data
-- Populates: cities, cinemas, screens, movies, seats, shows, show_seats
-- ============================================================

-- 1. CITIES
INSERT INTO cities (name, created_at, updated_at) VALUES
('Mumbai', NOW(), NOW()),
('Delhi', NOW(), NOW()),
('Bangalore', NOW(), NOW()),
('Hyderabad', NOW(), NOW()),
('Chennai', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- 2. MOVIES
INSERT INTO movies (title, description, language, genre, duration_minutes, release_date, poster_url) VALUES
('The Dark Knight Returns', 'Batman comes out of retirement in this thrilling sequel that challenges the very idea of justice.', 'English', 'Action', 152, '2026-03-01', 'https://placehold.co/300x450/1a1a2e/e23744?text=Dark+Knight'),
('Pushpa 3: The Rule Continues', 'Pushpa Raj returns to dominate the red sandalwood smuggling syndicate in an epic saga of power.', 'Telugu', 'Action', 168, '2026-02-14', 'https://placehold.co/300x450/1a1a2e/ff6b81?text=Pushpa+3'),
('Jawan 2', 'A man on a mission takes on systemic corruption with his band of women warriors.', 'Hindi', 'Thriller', 145, '2026-03-15', 'https://placehold.co/300x450/2d1b69/e23744?text=Jawan+2'),
('RRR: Rise Again', 'The legendary duo returns in an epic tale set during India''s fight for independence.', 'Telugu', 'Drama', 180, '2026-01-26', 'https://placehold.co/300x450/0d3b66/ff6b81?text=RRR+Rise'),
('Inception 2: Dreamscape', 'Cobb is pulled back into the dream world for one final impossible mission.', 'English', 'Sci-Fi', 158, '2026-03-20', 'https://placehold.co/300x450/1a1a2e/3498db?text=Inception+2'),
('Stree 3', 'The small town faces a new supernatural threat, bigger and funnier than ever before.', 'Hindi', 'Horror Comedy', 135, '2026-02-28', 'https://placehold.co/300x450/1a1a2e/2ecc71?text=Stree+3'),
('KGF Chapter 3', 'Rocky Bhai''s legacy is put to the ultimate test in this final chapter of the KGF saga.', 'Kannada', 'Action', 170, '2026-04-01', 'https://placehold.co/300x450/1a1a2e/f39c12?text=KGF+3'),
('Animal Park', 'A troubled son navigates the dark world of his father''s criminal empire.', 'Hindi', 'Crime', 155, '2026-03-10', 'https://placehold.co/300x450/1a1a2e/e74c3c?text=Animal+Park');

-- 3. CINEMAS (linked to cities)
INSERT INTO cinemas (name, address, city_id, created_at, updated_at) VALUES
('PVR Phoenix', 'Lower Parel, Mumbai', (SELECT id FROM cities WHERE name='Mumbai'), NOW(), NOW()),
('INOX Metro', 'Marine Lines, Mumbai', (SELECT id FROM cities WHERE name='Mumbai'), NOW(), NOW()),
('PVR Select City Walk', 'Saket, New Delhi', (SELECT id FROM cities WHERE name='Delhi'), NOW(), NOW()),
('Cinepolis DLF', 'Vasant Kunj, New Delhi', (SELECT id FROM cities WHERE name='Delhi'), NOW(), NOW()),
('INOX Garuda Mall', 'Magrath Road, Bangalore', (SELECT id FROM cities WHERE name='Bangalore'), NOW(), NOW()),
('PVR Orion', 'Rajajinagar, Bangalore', (SELECT id FROM cities WHERE name='Bangalore'), NOW(), NOW()),
('AMB Cinemas', 'Gachibowli, Hyderabad', (SELECT id FROM cities WHERE name='Hyderabad'), NOW(), NOW()),
('SPI Palazzo', 'Anna Nagar, Chennai', (SELECT id FROM cities WHERE name='Chennai'), NOW(), NOW());

-- 4. SCREENS (linked to cinemas)
INSERT INTO screens (name, total_seats, cinema_id) VALUES
('Screen 1 - IMAX', 120, (SELECT id FROM cinemas WHERE name='PVR Phoenix')),
('Screen 2', 80, (SELECT id FROM cinemas WHERE name='PVR Phoenix')),
('Screen 1 - 4DX', 100, (SELECT id FROM cinemas WHERE name='INOX Metro')),
('Audi 1 - Dolby', 90, (SELECT id FROM cinemas WHERE name='PVR Select City Walk')),
('Audi 2', 70, (SELECT id FROM cinemas WHERE name='PVR Select City Walk')),
('Screen 1', 110, (SELECT id FROM cinemas WHERE name='Cinepolis DLF')),
('Screen 1 - IMAX', 130, (SELECT id FROM cinemas WHERE name='INOX Garuda Mall')),
('Screen 2', 80, (SELECT id FROM cinemas WHERE name='INOX Garuda Mall')),
('Audi 1', 100, (SELECT id FROM cinemas WHERE name='PVR Orion')),
('Screen 1 - Dolby', 140, (SELECT id FROM cinemas WHERE name='AMB Cinemas')),
('Screen 1', 90, (SELECT id FROM cinemas WHERE name='SPI Palazzo'));

-- 5. SEATS (physical seats per screen — 20 seats per screen for demo)
DO $$
DECLARE
    scr RECORD;
    row_letter CHAR(1);
    seat_num INT;
    seat_type VARCHAR;
    rows_arr CHAR[] := ARRAY['A','B','C','D'];
BEGIN
    FOR scr IN (SELECT id FROM screens) LOOP
        FOREACH row_letter IN ARRAY rows_arr LOOP
            FOR seat_num IN 1..5 LOOP
                IF row_letter = 'A' THEN seat_type := 'VIP';
                ELSIF row_letter = 'B' THEN seat_type := 'PREMIUM';
                ELSE seat_type := 'REGULAR';
                END IF;

                INSERT INTO seats (seat_number, seat_type, screen_id)
                VALUES (row_letter || seat_num, seat_type, scr.id)
                ON CONFLICT (screen_id, seat_number) DO NOTHING;
            END LOOP;
        END LOOP;
    END LOOP;
END $$;

-- 6. SHOWS (schedule shows for today + next 3 days, multiple movies per screen)
DO $$
DECLARE
    scr RECORD;
    show_date DATE;
    base_hour INT;
    day_offset INT;
    movie_ids BIGINT[];
    mov_id BIGINT;
    idx INT;
    hours_arr INT[] := ARRAY[10, 14, 19];
    h INT;
BEGIN
    SELECT ARRAY_AGG(id ORDER BY id) INTO movie_ids FROM movies;

    idx := 0;
    FOR scr IN (SELECT id FROM screens ORDER BY id) LOOP
        FOR day_offset IN 0..3 LOOP
            show_date := CURRENT_DATE + day_offset;

            FOREACH h IN ARRAY hours_arr LOOP
                idx := idx + 1;
                mov_id := movie_ids[((idx - 1) % array_length(movie_ids, 1)) + 1];

                INSERT INTO shows (movie_id, screen_id, start_time, end_time, created_at, updated_at)
                VALUES (
                    mov_id,
                    scr.id,
                    (show_date + (h || ' hours')::INTERVAL),
                    (show_date + ((h + 2) || ' hours')::INTERVAL + '30 minutes'::INTERVAL),
                    NOW(), NOW()
                );
            END LOOP;
        END LOOP;
    END LOOP;
END $$;

-- 7. SHOW_SEATS (link every show to its screen's seats with pricing)
DO $$
DECLARE
    shw RECORD;
    st RECORD;
    price NUMERIC;
BEGIN
    FOR shw IN (SELECT s.id AS show_id, s.screen_id FROM shows s) LOOP
        FOR st IN (SELECT id, seat_type FROM seats WHERE screen_id = shw.screen_id) LOOP
            IF st.seat_type = 'VIP' THEN price := 500.00;
            ELSIF st.seat_type = 'PREMIUM' THEN price := 350.00;
            ELSE price := 200.00;
            END IF;

            INSERT INTO show_seats (show_id, seat_id, price, status)
            VALUES (shw.show_id, st.id, price, 'AVAILABLE')
            ON CONFLICT (show_id, seat_id) DO NOTHING;
        END LOOP;
    END LOOP;
END $$;

-- Summary count
SELECT 'cities' AS "table", COUNT(*) AS "rows" FROM cities
UNION ALL SELECT 'movies', COUNT(*) FROM movies
UNION ALL SELECT 'cinemas', COUNT(*) FROM cinemas
UNION ALL SELECT 'screens', COUNT(*) FROM screens
UNION ALL SELECT 'seats', COUNT(*) FROM seats
UNION ALL SELECT 'shows', COUNT(*) FROM shows
UNION ALL SELECT 'show_seats', COUNT(*) FROM show_seats;
