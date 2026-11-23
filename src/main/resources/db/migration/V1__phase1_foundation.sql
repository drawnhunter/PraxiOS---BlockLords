CREATE TABLE villages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL COLLATE NOCASE UNIQUE,
    owner_uuid TEXT NOT NULL,
    world_uuid TEXT NOT NULL,
    bell_x INTEGER NOT NULL,
    bell_y INTEGER NOT NULL,
    bell_z INTEGER NOT NULL,
    radius INTEGER NOT NULL CHECK (radius > 0),
    level INTEGER NOT NULL DEFAULT 1 CHECK (level >= 1),
    xp INTEGER NOT NULL DEFAULT 0 CHECK (xp >= 0),
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'RESTING', 'REVIEW')),
    created_at INTEGER NOT NULL,
    last_activity INTEGER NOT NULL
);

CREATE UNIQUE INDEX villages_bell_unique
    ON villages(world_uuid, bell_x, bell_y, bell_z);
CREATE INDEX villages_owner_idx ON villages(owner_uuid);
CREATE INDEX villages_region_idx ON villages(world_uuid, bell_x, bell_z);

CREATE TABLE members (
    village_id INTEGER NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    player_uuid TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('LANDHERR', 'GEFOLGSMANN', 'BUERGER', 'ASYL')),
    joined_at INTEGER NOT NULL,
    PRIMARY KEY (village_id, player_uuid)
);

CREATE INDEX members_player_idx ON members(player_uuid);
CREATE UNIQUE INDEX members_one_citizenship_idx
    ON members(player_uuid)
    WHERE role IN ('BUERGER', 'GEFOLGSMANN');

CREATE TABLE founding_quests (
    player_uuid TEXT PRIMARY KEY,
    stage TEXT NOT NULL CHECK (stage IN ('NOT_STARTED', 'SURVEY', 'READY')),
    bell_world TEXT,
    bell_x INTEGER,
    bell_y INTEGER,
    bell_z INTEGER,
    progress INTEGER NOT NULL DEFAULT 0,
    founder_completed INTEGER NOT NULL DEFAULT 0 CHECK (founder_completed IN (0, 1)),
    updated_at INTEGER NOT NULL
);

CREATE TABLE legacy_plots (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    owner_uuid TEXT,
    world_uuid TEXT NOT NULL,
    min_x INTEGER NOT NULL,
    min_y INTEGER NOT NULL,
    min_z INTEGER NOT NULL,
    max_x INTEGER NOT NULL,
    max_y INTEGER NOT NULL,
    max_z INTEGER NOT NULL,
    registered_by TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

CREATE INDEX legacy_plots_region_idx ON legacy_plots(world_uuid, min_x, min_z);

CREATE TABLE block_ownership (
    world_uuid TEXT NOT NULL,
    x INTEGER NOT NULL,
    y INTEGER NOT NULL,
    z INTEGER NOT NULL,
    owner_uuid TEXT NOT NULL,
    placed_at INTEGER NOT NULL,
    PRIMARY KEY (world_uuid, x, y, z)
);

CREATE INDEX block_ownership_region_idx
    ON block_ownership(world_uuid, x, z);

CREATE TABLE village_features (
    village_id INTEGER NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    feature_key TEXT NOT NULL,
    enabled INTEGER NOT NULL DEFAULT 0 CHECK (enabled IN (0, 1)),
    updated_at INTEGER NOT NULL,
    PRIMARY KEY (village_id, feature_key)
);

CREATE TABLE audit (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    actor_uuid TEXT,
    action TEXT NOT NULL,
    details TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

CREATE INDEX audit_created_idx ON audit(created_at DESC);
