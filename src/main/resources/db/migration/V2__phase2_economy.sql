CREATE TABLE buildings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    village_id INTEGER NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    type TEXT NOT NULL,
    level INTEGER NOT NULL DEFAULT 1,
    world_uuid TEXT NOT NULL,
    x INTEGER NOT NULL,
    y INTEGER NOT NULL,
    z INTEGER NOT NULL,
    rotation INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL
);

CREATE INDEX buildings_village_idx ON buildings(village_id);
CREATE INDEX buildings_location_idx ON buildings(world_uuid, x, z);

CREATE TABLE build_sites (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    village_id INTEGER NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    type TEXT NOT NULL,
    world_uuid TEXT NOT NULL,
    x INTEGER NOT NULL,
    y INTEGER NOT NULL,
    z INTEGER NOT NULL,
    rotation INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL CHECK (status IN ('COLLECTING', 'READY', 'BUILDING')),
    created_by TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

CREATE INDEX build_sites_village_idx ON build_sites(village_id);

CREATE TABLE build_site_resources (
    site_id INTEGER NOT NULL REFERENCES build_sites(id) ON DELETE CASCADE,
    resource TEXT NOT NULL,
    required INTEGER NOT NULL CHECK (required >= 0),
    contributed INTEGER NOT NULL DEFAULT 0 CHECK (contributed >= 0),
    PRIMARY KEY (site_id, resource)
);

CREATE TABLE build_site_contributors (
    site_id INTEGER NOT NULL REFERENCES build_sites(id) ON DELETE CASCADE,
    player_uuid TEXT NOT NULL,
    resource TEXT NOT NULL,
    amount INTEGER NOT NULL CHECK (amount > 0),
    contributed_at INTEGER NOT NULL
);

CREATE INDEX build_site_contributors_player_idx ON build_site_contributors(player_uuid);

CREATE TABLE storage (
    village_id INTEGER NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    resource TEXT NOT NULL,
    amount INTEGER NOT NULL DEFAULT 0 CHECK (amount >= 0),
    PRIMARY KEY (village_id, resource)
);

CREATE TABLE storage_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    village_id INTEGER NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    player_uuid TEXT,
    resource TEXT NOT NULL,
    delta INTEGER NOT NULL,
    reason TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

CREATE INDEX storage_transactions_village_idx ON storage_transactions(village_id, id DESC);

CREATE TABLE workers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    village_id INTEGER NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    villager_uuid TEXT NOT NULL UNIQUE,
    building_id INTEGER NOT NULL REFERENCES buildings(id) ON DELETE CASCADE,
    job TEXT NOT NULL CHECK (job IN ('FARMER', 'MINER')),
    assigned_at INTEGER NOT NULL
);

CREATE INDEX workers_village_idx ON workers(village_id);

CREATE TABLE production_state (
    village_id INTEGER PRIMARY KEY REFERENCES villages(id) ON DELETE CASCADE,
    last_cycle_at INTEGER NOT NULL,
    last_report TEXT,
    last_report_at INTEGER
);

CREATE TABLE production_totals (
    village_id INTEGER NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    resource TEXT NOT NULL,
    produced INTEGER NOT NULL DEFAULT 0,
    consumed INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (village_id, resource)
);

CREATE TABLE research (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    village_id INTEGER NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    research_key TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('RUNNING', 'COMPLETED', 'CANCELLED')),
    started_at INTEGER NOT NULL,
    finishes_at INTEGER NOT NULL,
    completed_at INTEGER
);

CREATE INDEX research_village_idx ON research(village_id, status);
