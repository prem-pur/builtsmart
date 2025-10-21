-- Migration: Add DEPARTED status to attendance table
-- Run this SQL script in your MySQL database

-- Option 1: If status column is VARCHAR
ALTER TABLE attendance MODIFY COLUMN status VARCHAR(20) NOT NULL;

-- Option 2: If status column is ENUM (use this if Option 1 doesn't work)
-- ALTER TABLE attendance MODIFY COLUMN status ENUM('PRESENT', 'ABSENT', 'LATE', 'HALF_DAY', 'ON_LEAVE', 'DEPARTED') NOT NULL;

-- Verify the change
DESCRIBE attendance;

-- Optional: View current attendance records
SELECT id, date, status, check_in, check_out FROM attendance ORDER BY date DESC LIMIT 10;
