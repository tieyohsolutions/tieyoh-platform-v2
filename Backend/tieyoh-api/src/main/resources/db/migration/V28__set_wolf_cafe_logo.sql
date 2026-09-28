-- The Wolf Cafe (Ballwin, MO) is a partnered merchant, but its logo_url
-- was never populated when it came into the registry through the
-- Outscraper import -- that bulk load isn't itself a Flyway migration
-- (see V1__create_merchant_registry.sql, which only creates the table
-- shape), so the row exists with logo_url still NULL.
--
-- Backfilling it here directly from the same source data
-- (Backend/merchant-imports/Ballwin_Data_Set.xlsx, "logo" column against
-- "The Wolf Cafe") rather than waiting on the merchant onboarding site --
-- once that's built, it'll capture and store logos for new partners
-- going forward; this is just getting the one partner Tieyoh already has
-- squared away in the meantime.
--
-- The URL is Google's own photo CDN
-- (https://lh5.googleusercontent.com/.../s44-p-k-no-ns-nd/photo.jpg).
-- Bumped the "s44" size segment up to "s200" so it renders crisp at the
-- app's ~40dp merchant-row icon size on high-density phones, instead of
-- the tiny 44px thumbnail Outscraper originally captured.
DO $$
DECLARE
    updated_count INTEGER;
BEGIN
    UPDATE merchant
    SET logo_url = 'https://lh5.googleusercontent.com/-RhHsJFAJ5d0/AAAAAAAAAAI/AAAAAAAAAAA/-ZB8Dz1YBSc/s200-p-k-no-ns-nd/photo.jpg',
        updated_at = CURRENT_TIMESTAMP
    WHERE brand_name = 'The Wolf Cafe';

    GET DIAGNOSTICS updated_count = ROW_COUNT;

    IF updated_count <> 1 THEN
        RAISE EXCEPTION 'Expected exactly 1 merchant named ''The Wolf Cafe'', found %. Check brand_name in the merchant table before re-running.', updated_count;
    END IF;
END $$;
