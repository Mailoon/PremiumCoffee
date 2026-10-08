-- A variant can only have one primary media asset, and `is_primary` must agree
-- with `role = 'PRIMARY'` instead of being a second, free-floating source of truth.
-- The 3D viewer picks the primary asset by `is_primary`, so an inconsistent row
-- left it choosing between thumbnails.

-- 1. Normalize before validating, so the check below cannot fail on legacy rows.
UPDATE product_variant_media
SET is_primary = (role = 'PRIMARY');

-- 2. Keep only the oldest row when a variant already claims several primaries.
DELETE FROM product_variant_media older USING product_variant_media newer
WHERE older.variant_id = newer.variant_id
  AND older.is_primary
  AND newer.is_primary
  AND (older.created_at, older.id) > (newer.created_at, newer.id);

-- 3. Enforce the rule. `23505` from the index and `23514` from the check are
-- already mapped to 409 by ApiExceptionHandler, so no Kotlin change is needed.
ALTER TABLE product_variant_media
    ADD CONSTRAINT chk_variant_media_primary_role CHECK (is_primary = (role = 'PRIMARY'));

CREATE UNIQUE INDEX uq_variant_media_one_primary
    ON product_variant_media (variant_id)
    WHERE is_primary;