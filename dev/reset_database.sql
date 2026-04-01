-- Reset the database: drops all tables and Flyway history so migrations run fresh.
-- Usage: psql -h localhost -U cookmaid -d cookmaid -f reset_database.sql

DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
