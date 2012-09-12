# --- !Ups
ALTER TABLE smarts_document ADD COLUMN features_bits_text TEXT;

# --- !Downs
ALTER TABLE smarts_document DROP COLUMN features_bits_text;