-- ==============================================================================
-- V3__seed_initial_data.sql
-- Seed initial data for BookMyShow Booking System
-- ==============================================================================

-- 1. CITIES
INSERT INTO cities (name, created_at, updated_at) VALUES
('Mumbai', NOW(), NOW()),
('Delhi', NOW(), NOW()),
('Bangalore', NOW(), NOW()),
('Hyderabad', NOW(), NOW()),
('Chennai', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- 2. MOVIES
INSERT INTO movies (title, description, language, genre, duration_minutes, release_date, poster_url, trailer_url, cast_members, rating) VALUES
('The Dark Knight Returns', 'Batman comes out of retirement in this thrilling sequel that challenges the very idea of justice.', 'English', 'Action', 152, '2026-03-01', '/posters/the-dark-knight-returns.jpg', 'https://www.youtube.com/embed/EXeTwQWrcwY', 'Christian Bale, Heath Ledger, Aaron Eckhart, Michael Caine, Gary Oldman', 9.0),
('Pushpa 3: The Rule Continues', 'Pushpa Raj returns to dominate the red sandalwood smuggling syndicate in an epic saga of power.', 'Telugu', 'Action', 168, '2026-02-14', '/posters/pushpa-3.jpg', 'https://www.youtube.com/embed/pKctjlxbFDc', 'Allu Arjun, Rashmika Mandanna, Fahadh Faasil', 8.3),
('Jawan 2', 'A man on a mission takes on systemic corruption with his band of women warriors.', 'Hindi', 'Thriller', 145, '2026-03-15', '/posters/jawan-2.jpg', 'https://www.youtube.com/embed/COv52Qyctws', 'Shah Rukh Khan, Nayanthara, Vijay Sethupathi, Deepika Padukone', 8.1),
('RRR: Rise Again', 'The legendary duo returns in an epic tale set during India''s fight for independence.', 'Telugu', 'Drama', 180, '2026-01-26', '/posters/rrr-rise-again.jpg', 'https://www.youtube.com/embed/f_vbAtFSEc0', 'N. T. Rama Rao Jr., Ram Charan, Ajay Devgn, Alia Bhatt', 8.8),
('Inception 2: Dreamscape', 'Cobb is pulled back into the dream world for one final impossible mission.', 'English', 'Sci-Fi', 158, '2026-03-20', '/posters/inception-2.jpg', 'https://www.youtube.com/embed/YoHD9XEInc0', 'Leonardo DiCaprio, Joseph Gordon-Levitt, Elliot Page, Tom Hardy', 8.9),
('Stree 3', 'The small town faces a new supernatural threat, bigger and funnier than ever before.', 'Hindi', 'Horror Comedy', 135, '2026-02-28', '/posters/stree-3.jpg', 'https://www.youtube.com/embed/cVYYtH7NR50', 'Rajkummar Rao, Shraddha Kapoor, Pankaj Tripathi', 7.8),
('KGF Chapter 3', 'Rocky Bhai''s legacy is put to the ultimate test in this final chapter of the KGF saga.', 'Kannada', 'Action', 170, '2026-04-01', '/posters/kgf-chapter-3.jpg', 'https://www.youtube.com/embed/P33uBY2Glkg', 'Yash, Sanjay Dutt, Srinidhi Shetty, Raveena Tandon', 8.6),
('Animal Park', 'A troubled son navigates the dark world of his father''s criminal empire.', 'Hindi', 'Crime', 155, '2026-03-10', '/posters/animal-park.jpg', 'https://www.youtube.com/embed/Q1NKMPhP8PY', 'Ranbir Kapoor, Anil Kapoor, Bobby Deol, Rashmika Mandanna', 8.0)
ON CONFLICT (title) DO NOTHING;

-- 3. CINEMAS
INSERT INTO cinemas (name, address, city_id, created_at, updated_at) VALUES
('PVR Phoenix', 'Lower Parel, Mumbai', (SELECT id FROM cities WHERE name='Mumbai'), NOW(), NOW()),
('INOX Metro', 'Marine Lines, Mumbai', (SELECT id FROM cities WHERE name='Mumbai'), NOW(), NOW()),
('PVR Select City Walk', 'Saket, New Delhi', (SELECT id FROM cities WHERE name='Delhi'), NOW(), NOW()),
('Cinepolis DLF', 'Vasant Kunj, New Delhi', (SELECT id FROM cities WHERE name='Delhi'), NOW(), NOW()),
('INOX Garuda Mall', 'Magrath Road, Bangalore', (SELECT id FROM cities WHERE name='Bangalore'), NOW(), NOW()),
('PVR Orion', 'Rajajinagar, Bangalore', (SELECT id FROM cities WHERE name='Bangalore'), NOW(), NOW()),
('AMB Cinemas', 'Gachibowli, Hyderabad', (SELECT id FROM cities WHERE name='Hyderabad'), NOW(), NOW()),
('SPI Palazzo', 'Anna Nagar, Chennai', (SELECT id FROM cities WHERE name='Chennai'), NOW(), NOW())
ON CONFLICT DO NOTHING;

-- 4. SCREENS
INSERT INTO screens (name, total_seats, cinema_id)
SELECT 'Screen 1 - IMAX', 120, id FROM cinemas WHERE name='PVR Phoenix'
ON CONFLICT DO NOTHING;

INSERT INTO screens (name, total_seats, cinema_id)
SELECT 'Screen 2', 80, id FROM cinemas WHERE name='PVR Phoenix'
ON CONFLICT DO NOTHING;

INSERT INTO screens (name, total_seats, cinema_id)
SELECT 'Screen 1 - 4DX', 100, id FROM cinemas WHERE name='INOX Metro'
ON CONFLICT DO NOTHING;

INSERT INTO screens (name, total_seats, cinema_id)
SELECT 'Audi 1 - Dolby', 90, id FROM cinemas WHERE name='PVR Select City Walk'
ON CONFLICT DO NOTHING;

INSERT INTO screens (name, total_seats, cinema_id)
SELECT 'Audi 2', 70, id FROM cinemas WHERE name='PVR Select City Walk'
ON CONFLICT DO NOTHING;

INSERT INTO screens (name, total_seats, cinema_id)
SELECT 'Screen 1', 110, id FROM cinemas WHERE name='Cinepolis DLF'
ON CONFLICT DO NOTHING;

INSERT INTO screens (name, total_seats, cinema_id)
SELECT 'Screen 1 - IMAX', 130, id FROM cinemas WHERE name='INOX Garuda Mall'
ON CONFLICT DO NOTHING;

INSERT INTO screens (name, total_seats, cinema_id)
SELECT 'Screen 2', 80, id FROM cinemas WHERE name='INOX Garuda Mall'
ON CONFLICT DO NOTHING;

INSERT INTO screens (name, total_seats, cinema_id)
SELECT 'Audi 1', 100, id FROM cinemas WHERE name='PVR Orion'
ON CONFLICT DO NOTHING;

INSERT INTO screens (name, total_seats, cinema_id)
SELECT 'Screen 1 - Dolby', 140, id FROM cinemas WHERE name='AMB Cinemas'
ON CONFLICT DO NOTHING;

INSERT INTO screens (name, total_seats, cinema_id)
SELECT 'Screen 1', 90, id FROM cinemas WHERE name='SPI Palazzo'
ON CONFLICT DO NOTHING;

-- 5. SEATS
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

-- 6. ADD-ONS (F&B Catalogue)
INSERT INTO add_ons (name, description, category, price, image_url, available, created_at, updated_at) VALUES
('Classic Salted Popcorn', 'Freshly popped warm buttery popcorn seasoned with sea salt.', 'FOOD', 180.00, 'https://images.unsplash.com/photo-1585647347483-22b66260dfff?w=500', TRUE, NOW(), NOW()),
('Cheesy Loaded Nachos', 'Crispy corn tortilla chips topped with warm jalapeño cheese sauce and salsa.', 'FOOD', 220.00, 'https://images.unsplash.com/photo-1513456852971-30c0b8199d4d?w=500', TRUE, NOW(), NOW()),
('Chilled Pepsi (Large)', 'Ice cold sparkling Pepsi 500ml.', 'BEVERAGE', 120.00, 'https://images.unsplash.com/photo-1629203851122-3726ecdf080e?w=500', TRUE, NOW(), NOW()),
('Cold Coffee Frappe', 'Rich creamy blended cold coffee with chocolate drizzle.', 'BEVERAGE', 160.00, 'https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5?w=500', TRUE, NOW(), NOW()),
('Cinema Combo (Popcorn + Drink)', 'Tub of salted popcorn paired with a large fountain beverage.', 'COMBO', 270.00, 'https://images.unsplash.com/photo-1505686994434-e3cc5abf1330?w=500', TRUE, NOW(), NOW())
ON CONFLICT DO NOTHING;
