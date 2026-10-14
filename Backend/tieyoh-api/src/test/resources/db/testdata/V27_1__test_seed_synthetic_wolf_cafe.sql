-- TEST-ONLY seed (DEF-001). Lives in src/test/resources, so it is never on
-- the application classpath and never runs against a real database; tests
-- add it to spring.flyway.locations (see TestcontainersConfiguration).
--
-- V28-V30 update the logo of an existing merchant named 'The Wolf Cafe' and
-- fail unless exactly one such row exists. On real databases that row comes
-- from the local-only merchant import, which is not a Flyway migration. A
-- fresh test database has no such row, so this inserts one synthetic
-- placeholder (name only, every other value made up) between V27 and V28.
-- Nothing here comes from Backend/merchant-imports/.
INSERT INTO merchant (tmid, brand_name, source, status)
VALUES ('TEST-SEED-0001', 'The Wolf Cafe', 'synthetic-test-seed', 'ACTIVE');
