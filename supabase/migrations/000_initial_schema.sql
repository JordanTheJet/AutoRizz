-- AutoRizz Database Schema
-- Supabase PostgreSQL

-- User profiles (extends Supabase auth.users)
CREATE TABLE user_profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name TEXT,
    credit_balance BIGINT DEFAULT 100,  -- welcome bonus: 100 credits
    total_credits_purchased BIGINT DEFAULT 0,
    subscription_plan TEXT DEFAULT 'free',
    subscription_status TEXT DEFAULT 'active',  -- active, canceling, past_due, canceled
    stripe_customer_id TEXT,
    stripe_subscription_id TEXT,
    current_period_start TIMESTAMPTZ,
    current_period_end TIMESTAMPTZ,
    rollover_credits BIGINT DEFAULT 0,
    ai_mode TEXT DEFAULT 'fast',
    referral_code TEXT UNIQUE DEFAULT gen_random_uuid()::TEXT,
    referred_by UUID REFERENCES auth.users(id),
    auto_refill_enabled BOOLEAN DEFAULT false,
    auto_refill_pack TEXT,
    auto_refill_threshold BIGINT,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Auto-create profile on user signup
CREATE OR REPLACE FUNCTION public.create_user_profile()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.user_profiles (id, credit_balance, subscription_plan)
    VALUES (NEW.id, 100, 'free');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION create_user_profile();

-- Credit transactions
CREATE TABLE credit_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    type TEXT NOT NULL,  -- 'purchase', 'usage', 'bonus', 'referral', 'refund'
    amount BIGINT NOT NULL,  -- positive for additions, negative for deductions
    balance_after BIGINT NOT NULL,
    description TEXT,
    stripe_payment_id TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Subscription plans catalog
CREATE TABLE subscription_plans (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    monthly_credits BIGINT NOT NULL,
    price_usd REAL NOT NULL,
    stripe_price_id TEXT,          -- NULL for free tier
    features JSONB DEFAULT '{}',   -- e.g. {"ai_modes": ["fast","good","thinking"]}
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Seed subscription plans
INSERT INTO subscription_plans (id, name, monthly_credits, price_usd, stripe_price_id, features) VALUES
    ('free',    'Free',    100,     0,     NULL,                                '{"ai_modes": ["fast"]}'),
    ('starter', 'Starter', 1000,    4.99,  'price_1T5tVfQWWPsMEhJU15QWTGXI',   '{"ai_modes": ["fast", "good"]}'),
    ('pro',     'Pro',     10000,   19.99, 'price_1T5tVmQWWPsMEhJUC8ntvGR9',   '{"ai_modes": ["fast", "good", "thinking"]}'),
    ('ultra',   'Ultra',   100000,  99.00, 'price_1T5tVoQWWPsMEhJUvhyRgsVV',   '{"ai_modes": ["fast", "good", "thinking"]}');

-- Conversations
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    deleted_at TIMESTAMPTZ  -- soft delete
);

-- Messages
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    role TEXT NOT NULL,  -- 'user', 'assistant', 'tool_use', 'tool_result'
    content JSONB NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    device_id TEXT
);

-- Memory facts
CREATE TABLE memory_facts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    key TEXT NOT NULL,
    value TEXT NOT NULL,
    category TEXT,
    confidence REAL DEFAULT 1.0,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- User settings (synced)
CREATE TABLE user_settings (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    settings JSONB NOT NULL DEFAULT '{}',
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Skills configuration
CREATE TABLE user_skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    skill_name TEXT NOT NULL,
    skill_content TEXT NOT NULL,
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Scheduled tasks
CREATE TABLE scheduled_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    task_description TEXT NOT NULL,
    cron_expression TEXT,
    one_shot_at TIMESTAMPTZ,
    enabled BOOLEAN DEFAULT true,
    last_run_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Credit packs catalog
CREATE TABLE credit_packs (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    credits BIGINT NOT NULL,
    price_usd REAL NOT NULL,
    stripe_price_id TEXT,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Usage logs
CREATE TABLE usage_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    provider TEXT NOT NULL,
    model TEXT NOT NULL,
    input_tokens BIGINT NOT NULL,
    output_tokens BIGINT NOT NULL,
    cost_usd REAL,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Row-level security
ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE credit_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE memory_facts ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_skills ENABLE ROW LEVEL SECURITY;
ALTER TABLE scheduled_tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE usage_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscription_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE credit_packs ENABLE ROW LEVEL SECURITY;

-- RLS policies: users can only access their own data
CREATE POLICY "Users own data" ON user_profiles FOR ALL USING (auth.uid() = id);
CREATE POLICY "Users own data" ON credit_transactions FOR ALL USING (auth.uid() = user_id);
CREATE POLICY "Users own data" ON conversations FOR ALL USING (auth.uid() = user_id);
CREATE POLICY "Users own data" ON messages FOR ALL USING (auth.uid() = user_id);
CREATE POLICY "Users own data" ON memory_facts FOR ALL USING (auth.uid() = user_id);
CREATE POLICY "Users own data" ON user_settings FOR ALL USING (auth.uid() = user_id);
CREATE POLICY "Users own data" ON user_skills FOR ALL USING (auth.uid() = user_id);
CREATE POLICY "Users own data" ON scheduled_tasks FOR ALL USING (auth.uid() = user_id);
CREATE POLICY "Users own data" ON usage_logs FOR ALL USING (auth.uid() = user_id);
-- Subscription plans and credit packs are public read
CREATE POLICY "Anyone can read plans" ON subscription_plans FOR SELECT USING (true);
CREATE POLICY "Anyone can read packs" ON credit_packs FOR SELECT USING (true);

-- Indexes
CREATE INDEX idx_credit_transactions_user ON credit_transactions(user_id, created_at DESC);
CREATE INDEX idx_conversations_user ON conversations(user_id, updated_at DESC);
CREATE INDEX idx_messages_conversation ON messages(conversation_id, created_at ASC);
CREATE INDEX idx_memory_facts_user ON memory_facts(user_id, updated_at DESC);
CREATE INDEX idx_usage_logs_user ON usage_logs(user_id, created_at DESC);
