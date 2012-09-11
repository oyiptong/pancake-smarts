# --- !Ups
ALTER TABLE smarts_document ADD COLUMN features_text TEXT, ADD COLUMN features_bits BLOB;

# --- !Downs
ALTER TABLE smarts_document DROP COLUMN features_text, DROP COLUMN features_bits;