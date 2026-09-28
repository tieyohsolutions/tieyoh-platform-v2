-- Registered/partnered merchants are the ones that have signed up to
-- accept a scanned Tieyoh payment QR code (see PayWithQrScreen.kt's
-- "Find Partnered Merchants" and MerchantRepository.
-- searchEligiblePartneredMerchants / findNearbyPartnered). Every merchant
-- in the registry defaults to NOT partnered -- being in the registry
-- (e.g. from the Outscraper import) only means Tieyoh knows the business
-- exists, not that it accepts Tieyoh payments yet.
ALTER TABLE merchant ADD COLUMN partnered BOOLEAN NOT NULL DEFAULT FALSE;
