CREATE TABLE missions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    village_id INTEGER NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    player_uuid TEXT,
    mission_type TEXT NOT NULL,
    scope TEXT NOT NULL CHECK (scope IN ('PERSONAL', 'DORF')),
    target INTEGER NOT NULL CHECK (target > 0),
    xp INTEGER NOT NULL CHECK (xp >= 0),
    status TEXT NOT NULL CHECK (status IN ('OFFERED', 'ACTIVE', 'COMPLETED', 'REPLACED')),
    progress INTEGER NOT NULL DEFAULT 0 CHECK (progress >= 0 AND progress <= target),
    created_at INTEGER NOT NULL,
    accepted_at INTEGER,
    completed_at INTEGER,
    completion_notified_at INTEGER,
    pinned INTEGER NOT NULL DEFAULT 0 CHECK (pinned IN (0, 1)),
    CHECK ((scope = 'PERSONAL' AND player_uuid IS NOT NULL) OR (scope = 'DORF' AND player_uuid IS NULL))
);

CREATE INDEX missions_recipient_idx ON missions(village_id, player_uuid, status);
CREATE UNIQUE INDEX missions_personal_current_unique ON missions(village_id, player_uuid, mission_type)
    WHERE scope = 'PERSONAL' AND status IN ('OFFERED', 'ACTIVE');
CREATE UNIQUE INDEX missions_village_current_unique ON missions(village_id, mission_type)
    WHERE scope = 'DORF' AND status IN ('OFFERED', 'ACTIVE');
CREATE UNIQUE INDEX missions_personal_pin_unique ON missions(village_id, player_uuid) WHERE pinned = 1;

CREATE TABLE mission_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_key TEXT NOT NULL UNIQUE,
    village_id INTEGER NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    player_uuid TEXT NOT NULL,
    action TEXT NOT NULL CHECK (action IN ('STORAGE_DEPOSIT', 'BUILD_CONTRIBUTION')),
    resource TEXT,
    amount INTEGER NOT NULL CHECK (amount > 0),
    source_reference TEXT,
    created_at INTEGER NOT NULL
);

CREATE INDEX mission_events_village_idx ON mission_events(village_id, id);

CREATE TABLE mission_event_links (
    event_id INTEGER NOT NULL REFERENCES mission_events(id) ON DELETE CASCADE,
    mission_id INTEGER NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
    amount INTEGER NOT NULL CHECK (amount > 0),
    created_at INTEGER NOT NULL,
    PRIMARY KEY (event_id, mission_id)
);

CREATE INDEX mission_event_links_mission_idx ON mission_event_links(mission_id);

CREATE TABLE mission_xp_grants (
    village_id INTEGER NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    recipient_key TEXT NOT NULL,
    mission_type TEXT NOT NULL,
    mission_id INTEGER NOT NULL UNIQUE REFERENCES missions(id) ON DELETE CASCADE,
    xp INTEGER NOT NULL CHECK (xp >= 0),
    created_at INTEGER NOT NULL,
    PRIMARY KEY (village_id, recipient_key, mission_type)
);
