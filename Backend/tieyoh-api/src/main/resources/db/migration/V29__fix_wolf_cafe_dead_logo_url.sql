-- V28 set logo_url to a Google Business "logo" CDN link scraped by
-- Outscraper into Ballwin_Data_Set.xlsx. That specific link has since
-- rotted -- confirmed by fetching it directly, it now 404s, and so does
-- the original un-resized URL from the spreadsheet before V28 touched it.
-- This isn't something V28's resize edit caused; these old-style
-- lh3/lh5.googleusercontent.com "-XXXX/AAAAAAAAAAI/.../sNN.../photo.jpg"
-- links are known to expire independent of anything done to them, since
-- they're internal Google references never meant to be hotlinked
-- long-term by a third party.
--
-- Switching to the same row's "photo" column instead (a different URL
-- format, gps-cs-s/AHRPTWm.../=w800-h500-k-no), which is still alive as
-- of this migration. It's a wider storefront/interior photo rather than
-- a clean square logo, so it'll get center-cropped by the app's
-- ContentScale.Crop -- not perfect, but a real photo of the actual place
-- beats a blank space. This is exactly the kind of gap the merchant
-- onboarding site (once built) replaces for good, by having partners
-- upload their own logo file to storage Tieyoh actually controls instead
-- of hotlinking someone else's CDN.
DO $$
DECLARE
    updated_count INTEGER;
BEGIN
    UPDATE merchant
    SET logo_url = 'https://lh3.googleusercontent.com/gps-cs-s/AHRPTWmjzVjQxiqv3cnGR9I1Bjz6QzGn5b_RsGRi73zYVuCqhPeVpMfi5kYpW53VbWmI6o73zkaCBEXHDHQitXB-UlAK3UMmIpHQWLUHiXfHXo3-sPJ_pGYMpoJnJeu5RhPjYpSbZQ=w400-h400-k-no',
        updated_at = CURRENT_TIMESTAMP
    WHERE brand_name = 'The Wolf Cafe';

    GET DIAGNOSTICS updated_count = ROW_COUNT;

    IF updated_count <> 1 THEN
        RAISE EXCEPTION 'Expected exactly 1 merchant named ''The Wolf Cafe'', found %. Check brand_name in the merchant table before re-running.', updated_count;
    END IF;
END $$;
