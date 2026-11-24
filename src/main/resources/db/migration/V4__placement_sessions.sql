CREATE TABLE placement_sessions (
    player_uuid TEXT PRIMARY KEY,
    village_id INTEGER NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    blueprint TEXT NOT NULL,
    contents BLOB NOT NULL,
    armor BLOB NOT NULL,
    extras BLOB NOT NULL,
    selected_slot INTEGER NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('ACTIVE', 'CLOSED')),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX placement_sessions_status_idx ON placement_sessions(status);
