-- V28 and V29 both sourced Wolf Cafe's logo from Outscraper-scraped Google
-- Business Profile CDN links (lh5.googleusercontent.com, then
-- lh3.googleusercontent.com/gps-cs-s/...). Both of those are third-party
-- scraped references, not something Tieyoh controls, and this class of
-- URL has already rotted once in this repo's history.
--
-- For demo purposes, switching to a URL pulled directly from the
-- merchant's own site (thewolfstl.com), served through WordPress.com's
-- Photon image CDN (i0.wp.com). This is still a hotlinked third-party
-- URL -- not something Tieyoh controls or has uploaded -- so it carries
-- the same long-term fragility as V28/V29. It's fine for a demo; once
-- the merchant onboarding site exists, partners will upload their own
-- logo file to storage Tieyoh actually owns, which is the real fix for
-- this whole class of problem.
DO $$
DECLARE
    updated_count INTEGER;
BEGIN
    UPDATE merchant
    SET logo_url = 'https://i0.wp.com/thewolfstl.com/wp-content/uploads/2019/04/wolf-bee-tag-web.png?ssl=1',
        updated_at = CURRENT_TIMESTAMP
    WHERE brand_name = 'The Wolf Cafe';

    GET DIAGNOSTICS updated_count = ROW_COUNT;

    IF updated_count <> 1 THEN
        RAISE EXCEPTION 'Expected exactly 1 merchant named ''The Wolf Cafe'', found %. Check brand_name in the merchant table before re-running.', updated_count;
    END IF;
END $$;
