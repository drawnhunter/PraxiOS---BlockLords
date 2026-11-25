ALTER TABLE residents ADD COLUMN display_name TEXT;
ALTER TABLE residents ADD COLUMN resident_number INTEGER NOT NULL DEFAULT 0;

CREATE INDEX residents_number_idx ON residents(village_id, resident_number);
