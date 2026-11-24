CREATE TABLE residents (
    village_id INTEGER NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    villager_uuid TEXT NOT NULL PRIMARY KEY,
    status TEXT NOT NULL CHECK (status IN ('CONFIRMED', 'UNCONFIRMED')),
    has_bed INTEGER NOT NULL DEFAULT 0 CHECK (has_bed IN (0, 1)),
    bed_world TEXT,
    bed_x INTEGER,
    bed_y INTEGER,
    bed_z INTEGER,
    meeting_world TEXT,
    meeting_x INTEGER,
    meeting_y INTEGER,
    meeting_z INTEGER,
    first_seen INTEGER NOT NULL,
    last_seen INTEGER NOT NULL
);

CREATE INDEX residents_village_idx ON residents(village_id);
