-- SQL Script to Populate Seat Table
-- This script creates 10 Movie resources and 200 available seats for each.

-- 1. Ensure 'Movies' category exists
INSERT INTO categories (name, description, created_at, updated_at)
VALUES ('Movies', 'Book tickets for the latest movies in theaters near you.', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- 2. Populate 10 Movies and 200 seats per movie
DO $$
DECLARE
    cat_id BIGINT;
    res_id BIGINT;
    i INT;
    s INT;
BEGIN
    -- Get the category ID
    SELECT id INTO cat_id FROM categories WHERE name = 'Movies' LIMIT 1;

    FOR i IN 1..10 LOOP
        -- Create record if it doesn't exist
        IF NOT EXISTS (SELECT 1 FROM bookable_resources WHERE name = 'Movie ' || i) THEN
            INSERT INTO bookable_resources (name, type, capacity, is_active, price, category_id, created_at, updated_at)
            VALUES ('Movie ' || i, 'MOVIE', 200, true, 450.00, cat_id, NOW(), NOW())
            RETURNING resource_id INTO res_id;

            -- Create 200 seats for each movie
            FOR s IN 1..200 LOOP
                INSERT INTO seats (resource_id, seat_number, status, created_at, updated_at)
                VALUES (res_id, 'S' || s, 'AVAILABLE', NOW(), NOW());
            END LOOP;
        END IF;
    END LOOP;
END $$;
