-- App version configuration for forced/suggested updates
CREATE TABLE app_config (
    key TEXT PRIMARY KEY,
    value JSONB NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Seed the version config
INSERT INTO app_config (key, value) VALUES (
    'app_version',
    '{
        "min_version_code": 1,
        "latest_version_code": 1,
        "latest_version_name": "0.1.0",
        "update_url": "https://play.google.com/store/apps/details?id=com.autorizz",
        "update_message": ""
    }'::jsonb
);

-- Public read, no auth needed to check version
ALTER TABLE app_config ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone can read app config" ON app_config FOR SELECT USING (true);
