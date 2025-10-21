-- Fix database schema issues
-- Run this SQL script to fix the worker_id column issues

-- Fix attendance table
ALTER TABLE attendance DROP COLUMN IF EXISTS worker_id;

-- Fix leave_requests table  
ALTER TABLE leave_requests DROP COLUMN IF EXISTS worker_id;

-- Verify the tables
DESCRIBE attendance;
DESCRIBE leave_requests;
