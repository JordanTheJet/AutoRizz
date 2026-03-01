-- ============================================
-- DATING-SPECIFIC TABLES
-- AutoRizz dating automation data model
-- ============================================

-- Swipe preferences (key-value pairs per user)
CREATE TABLE swipe_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    key TEXT NOT NULL,
    value TEXT NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE(user_id, key)
);

-- Matches from dating apps
CREATE TABLE matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    app TEXT NOT NULL,  -- hinge / tinder / bumble
    age INTEGER,
    bio_summary TEXT,
    status TEXT NOT NULL DEFAULT 'new',  -- new / conversing / date_scheduled / date_completed / stale / unmatched
    matched_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_message_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Conversation messages (dating app messages, not agent messages)
CREATE TABLE match_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    match_id UUID REFERENCES matches(id) ON DELETE CASCADE,
    direction TEXT NOT NULL,  -- sent / received
    content TEXT NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Scheduled dates
CREATE TABLE scheduled_dates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    match_id UUID REFERENCES matches(id) ON DELETE CASCADE,
    date_time TIMESTAMPTZ NOT NULL,
    location TEXT,
    notes TEXT,
    status TEXT NOT NULL DEFAULT 'scheduled',  -- scheduled / completed / cancelled / no_show
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Swipe session logs
CREATE TABLE swipe_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    app TEXT NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at TIMESTAMPTZ,
    profiles_seen INTEGER DEFAULT 0,
    likes INTEGER DEFAULT 0,
    passes INTEGER DEFAULT 0,
    hit_limit BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================
-- INDEXES
-- ============================================

CREATE INDEX idx_matches_user_status ON matches(user_id, status);
CREATE INDEX idx_matches_user_app ON matches(user_id, app);
CREATE INDEX idx_match_messages_match ON match_messages(match_id);
CREATE INDEX idx_match_messages_user ON match_messages(user_id);
CREATE INDEX idx_scheduled_dates_user ON scheduled_dates(user_id);
CREATE INDEX idx_scheduled_dates_match ON scheduled_dates(match_id);
CREATE INDEX idx_swipe_sessions_user_app ON swipe_sessions(user_id, app);

-- ============================================
-- ROW LEVEL SECURITY
-- ============================================

ALTER TABLE swipe_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE matches ENABLE ROW LEVEL SECURITY;
ALTER TABLE match_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE scheduled_dates ENABLE ROW LEVEL SECURITY;
ALTER TABLE swipe_sessions ENABLE ROW LEVEL SECURITY;

-- Users can only access their own data
CREATE POLICY "Users can manage own swipe_preferences"
    ON swipe_preferences FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can manage own matches"
    ON matches FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can manage own match_messages"
    ON match_messages FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can manage own scheduled_dates"
    ON scheduled_dates FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can manage own swipe_sessions"
    ON swipe_sessions FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);
