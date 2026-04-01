-- ====================================================================
-- Migration Script: Phase 1 → Phase 2
-- Cleans up old seat data tied to BookableResource
-- and prepares the seats table for the new Screen-based architecture.
-- ====================================================================

-- 1. Drop old columns from seats that no longer exist in the new entity
--    (status, locked_at, locked_by_user_id, resource_id were removed)
ALTER TABLE seats DROP CONSTRAINT IF EXISTS seats_resource_id_seat_number_key;
ALTER TABLE seats DROP COLUMN IF EXISTS resource_id;
ALTER TABLE seats DROP COLUMN IF EXISTS status;
ALTER TABLE seats DROP COLUMN IF EXISTS locked_at;
ALTER TABLE seats DROP COLUMN IF EXISTS locked_by_user_id;

-- 2. Delete all old seat rows (they were tied to BookableResource, not Screen)
DELETE FROM seats;

-- 3. Add the new columns if they don't already exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='seats' AND column_name='screen_id') THEN
        ALTER TABLE seats ADD COLUMN screen_id BIGINT NOT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='seats' AND column_name='seat_type') THEN
        ALTER TABLE seats ADD COLUMN seat_type VARCHAR(255) NOT NULL DEFAULT 'REGULAR'
            CHECK (seat_type IN ('REGULAR','PREMIUM','VIP','RECLINER'));
    END IF;
END $$;

-- 4. Add unique constraint for screen_id + seat_number
ALTER TABLE seats DROP CONSTRAINT IF EXISTS ukebiyse65sapubpxt8cos1hkg2;
ALTER TABLE seats ADD CONSTRAINT uk_seats_screen_seat UNIQUE (screen_id, seat_number);

-- 5. Add FK to screens table
ALTER TABLE seats DROP CONSTRAINT IF EXISTS fkle5tj2wlw9xe9wat223f2dq8j;
ALTER TABLE seats ADD CONSTRAINT fk_seats_screen
    FOREIGN KEY (screen_id) REFERENCES screens(id);

-- Done! Restart the application and Hibernate will be happy.
