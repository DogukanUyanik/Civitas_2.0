-- Civitas: introduce the VIEWER role (replaces the never-assigned USER role).
--
-- ###########################################################################
-- #  TAKE A DATABASE BACKUP BEFORE RUNNING THIS SCRIPT.                     #
-- #  e.g.  mysqldump --single-transaction civitas_db2 > before_viewer.sql   #
-- ###########################################################################
--
-- Why it is needed: Hibernate 6.6 maps @Enumerated(EnumType.STRING) to a native MySQL ENUM column,
-- and spring.jpa.hibernate.ddl-auto=update never alters an existing column. On an already-created
-- database, saving a VIEWER would fail with "Data truncated for column 'role'" (and any leftover
-- USER row would fail to load once USER is removed from the Java enum).
--
-- When to run: BEFORE deploying the build that contains the VIEWER role.
-- Idempotent: safe to run more than once. Fresh databases (ddl-auto=create-drop) don't need it.
-- Tested against a throwaway MySQL 8.0 container by UserRoleEnumMigrationIntegrationTest.

-- 1. Widen the column so both the old and the new values are legal.
ALTER TABLE users MODIFY COLUMN role ENUM('USER','ADMIN','VIEWER') NOT NULL;

-- 2. USER never had any special meaning (no code assigned or checked it); it is the non-admin role.
UPDATE users SET role = 'VIEWER' WHERE role = 'USER';

-- 3. Narrow the column to the values the application now knows.
ALTER TABLE users MODIFY COLUMN role ENUM('ADMIN','VIEWER') NOT NULL;
