-- member.language must be NOT NULL, as the entity requires (@NotNull on Member.language).
--
-- Why: the column was added to an existing table by ddl-auto=update before @NotNull existed, so on
-- the production database it was created nullable (DEFAULT NULL). Databases whose member table was
-- created later (and V1__baseline.sql) already have NOT NULL; there this is a no-op.
--
-- No backfill: production was checked on 2026-10-03 and has 0 rows with language IS NULL. If a
-- database does contain NULLs this statement fails rather than guessing a language for them.
ALTER TABLE member MODIFY COLUMN language ENUM('EN','NL','TR') NOT NULL;
