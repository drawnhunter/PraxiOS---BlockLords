CREATE TABLE immigration_state (
    village_id INTEGER PRIMARY KEY REFERENCES villages(id) ON DELETE CASCADE,
    next_arrival_at INTEGER NOT NULL DEFAULT 0,
    reason TEXT NOT NULL DEFAULT '',
    updated_at INTEGER NOT NULL
);

CREATE TABLE immigration_candidates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    village_id INTEGER NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    villager_uuid TEXT NOT NULL UNIQUE,
    status TEXT NOT NULL CHECK (status IN ('WAITING', 'ACCEPTED', 'REJECTED', 'LOST', 'CANCELLED')),
    created_at INTEGER NOT NULL,
    decided_at INTEGER,
    xp_awarded INTEGER NOT NULL DEFAULT 0 CHECK (xp_awarded >= 0)
);

CREATE INDEX immigration_candidates_village_idx ON immigration_candidates(village_id, status);
CREATE UNIQUE INDEX immigration_one_waiting ON immigration_candidates(village_id) WHERE status = 'WAITING';

CREATE TABLE production_reports (
    village_id INTEGER PRIMARY KEY REFERENCES villages(id) ON DELETE CASCADE,
    recorded_at INTEGER NOT NULL,
    cycle_at INTEGER NOT NULL,
    active INTEGER NOT NULL CHECK (active IN (0, 1)),
    effective_cycles REAL NOT NULL CHECK (effective_cycles >= 0),
    farmers INTEGER NOT NULL CHECK (farmers >= 0),
    miners INTEGER NOT NULL CHECK (miners >= 0),
    food_consumed INTEGER NOT NULL CHECK (food_consumed >= 0),
    pauses TEXT NOT NULL DEFAULT ''
);

CREATE TABLE production_report_resources (
    village_id INTEGER NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    resource TEXT NOT NULL,
    planned INTEGER NOT NULL CHECK (planned >= 0),
    accepted INTEGER NOT NULL CHECK (accepted >= 0),
    PRIMARY KEY (village_id, resource)
);
