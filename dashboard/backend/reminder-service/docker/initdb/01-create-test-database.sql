-- Runs once, when the Postgres data volume is first created.
-- The integration tests wipe this database, so it is kept separate from "reminders".
CREATE DATABASE reminders_test;
