CREATE EXTENSION IF NOT EXISTS postgis;

-- Eatery types (lookup table for eatery categories like "Hawker Stall")
CREATE TABLE IF NOT EXISTS eatery_types (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    label varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

-- Foods (lookup table for food items like "Chicken Rice")
CREATE TABLE IF NOT EXISTS foods (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    label varchar(255) NOT NULL,
    photo_obj_key varchar(255),
    PRIMARY KEY (id)
);

-- Eateries (the actual dining locations)
CREATE TABLE IF NOT EXISTS eateries (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    address varchar(255),
    is_open boolean NOT NULL DEFAULT TRUE,
    location geometry(Point, 4326) NOT NULL,
    name varchar(255) NOT NULL,
    photo_obj_key varchar(255),
    type_id uuid NOT NULL,
    PRIMARY KEY (id)
);

-- Food entries (price submissions for a food at an eatery)
CREATE TABLE IF NOT EXISTS food_entries (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    downvote_count integer NOT NULL DEFAULT 0,
    sg_cents integer NOT NULL,
    submitter_id uuid NOT NULL,
    upvote_count integer NOT NULL DEFAULT 0,
    eatery_id uuid NOT NULL,
    food_id uuid NOT NULL,
    PRIMARY KEY (id)
);

-- Food entry flags (reports of incorrect prices)
CREATE TABLE IF NOT EXISTS food_entry_flags (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    flagger_id uuid NOT NULL,
    reason varchar(512) NOT NULL,
    food_entry_id uuid NOT NULL,
    PRIMARY KEY (id)
);

-- Food entry votes (upvote/downvote on price submissions)
CREATE TABLE IF NOT EXISTS food_entry_votes (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    is_upvote boolean NOT NULL,
    voter_id uuid NOT NULL,
    food_entry_id uuid NOT NULL,
    PRIMARY KEY (id)
);

-- Eatery closure flags (reports of permanently closed eateries)
CREATE TABLE IF NOT EXISTS eatery_closure_flags (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    flagger_id uuid,
    eatery_id uuid,
    PRIMARY KEY (id)
);

-- Unique constraints (DO block needed: PostgreSQL doesn't support IF NOT EXISTS on ALTER TABLE ADD CONSTRAINT)
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_foods_label') THEN ALTER TABLE foods ADD CONSTRAINT uk_foods_label UNIQUE (label); END IF; END; $$;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_eatery_types_label') THEN ALTER TABLE eatery_types ADD CONSTRAINT uk_eatery_types_label UNIQUE (label); END IF; END; $$;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_food_entry_votes_voter_entry') THEN ALTER TABLE food_entry_votes ADD CONSTRAINT uk_food_entry_votes_voter_entry UNIQUE (voter_id, food_entry_id); END IF; END; $$;

-- Foreign keys
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_eateries_type_id') THEN ALTER TABLE eateries ADD CONSTRAINT fk_eateries_type_id FOREIGN KEY (type_id) REFERENCES eatery_types (id); END IF; END; $$;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_food_entries_eatery_id') THEN ALTER TABLE food_entries ADD CONSTRAINT fk_food_entries_eatery_id FOREIGN KEY (eatery_id) REFERENCES eateries (id); END IF; END; $$;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_food_entries_food_id') THEN ALTER TABLE food_entries ADD CONSTRAINT fk_food_entries_food_id FOREIGN KEY (food_id) REFERENCES foods (id); END IF; END; $$;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_food_entry_flags_food_entry_id') THEN ALTER TABLE food_entry_flags ADD CONSTRAINT fk_food_entry_flags_food_entry_id FOREIGN KEY (food_entry_id) REFERENCES food_entries (id); END IF; END; $$;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_food_entry_votes_food_entry_id') THEN ALTER TABLE food_entry_votes ADD CONSTRAINT fk_food_entry_votes_food_entry_id FOREIGN KEY (food_entry_id) REFERENCES food_entries (id); END IF; END; $$;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_eatery_closure_flags_eatery_id') THEN ALTER TABLE eatery_closure_flags ADD CONSTRAINT fk_eatery_closure_flags_eatery_id FOREIGN KEY (eatery_id) REFERENCES eateries (id); END IF; END; $$;

-- Indexes (improves query performance on FK lookups)
CREATE INDEX IF NOT EXISTS idx_eateries_type_id ON eateries (type_id);
CREATE INDEX IF NOT EXISTS idx_food_entries_eatery_id ON food_entries (eatery_id);
CREATE INDEX IF NOT EXISTS idx_food_entries_food_id ON food_entries (food_id);
CREATE INDEX IF NOT EXISTS idx_food_entry_flags_food_entry_id ON food_entry_flags (food_entry_id);
CREATE INDEX IF NOT EXISTS idx_food_entry_votes_food_entry_id ON food_entry_votes (food_entry_id);
CREATE INDEX IF NOT EXISTS idx_food_entry_votes_voter_id ON food_entry_votes (voter_id);
CREATE INDEX IF NOT EXISTS idx_eatery_closure_flags_eatery_id ON eatery_closure_flags (eatery_id);
