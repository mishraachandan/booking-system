-- ============================================================
-- Refresh Shows: push all shows to future dates (next 4 days)
-- and reset all show_seats back to AVAILABLE
-- ============================================================

-- Step 1: Release all locked / booked show_seats back to AVAILABLE
UPDATE show_seats
SET status = 'AVAILABLE',
    locked_at = NULL,
    locked_by_user_id = NULL,
    booking_id = NULL;

-- Step 2: Delete ALL existing shows and show_seats (we'll recreate them)
DELETE FROM show_seats;
DELETE FROM shows;

-- Step 3: Recreate shows scheduled for today+1 through today+4
DO $$
DECLARE
    scr RECORD;
    show_date DATE;
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
        FOR day_offset IN 1..4 LOOP
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

-- Step 4: Recreate show_seats for every new show
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

-- Summary
SELECT 'shows' AS "table", COUNT(*) AS "rows" FROM shows
UNION ALL SELECT 'show_seats', COUNT(*) FROM show_seats;
