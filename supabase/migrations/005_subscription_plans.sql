-- Migrate from one-time credit packs to subscription plans
-- Each subscription gives a monthly credit allocation
-- Free plan: no rollover, credits simply reset each cycle
-- Paid plans: unused credits roll over, capped at 2× monthly allocation
-- Downgrade/cancel: paid credits expire, balance resets to free allocation

-- 1. Create subscription_plans table
CREATE TABLE subscription_plans (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    monthly_credits BIGINT NOT NULL,
    price_usd REAL NOT NULL,
    stripe_price_id TEXT,          -- Stripe recurring price ID
    features JSONB DEFAULT '{}',   -- e.g. {"ai_modes": ["fast","good","thinking"]}
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Public read access
ALTER TABLE subscription_plans ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone can read plans" ON subscription_plans FOR SELECT USING (true);

-- 2. Seed subscription plans
INSERT INTO subscription_plans (id, name, monthly_credits, price_usd, stripe_price_id, features) VALUES
    ('free', 'Free', 100, 0, NULL, '{"ai_modes": ["fast"]}'),
    ('starter', 'Starter', 1000, 4.99, 'price_1T5tVfQWWPsMEhJU15QWTGXI', '{"ai_modes": ["fast", "good"]}'),
    ('pro', 'Pro', 10000, 19.99, 'price_1T5tVmQWWPsMEhJUC8ntvGR9', '{"ai_modes": ["fast", "good", "thinking"]}'),
    ('ultra', 'Ultra', 100000, 99.00, 'price_1T5tVoQWWPsMEhJUvhyRgsVV', '{"ai_modes": ["fast", "good", "thinking"]}');

-- 3. Add subscription fields to user_profiles
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS subscription_plan TEXT DEFAULT 'free';
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS stripe_customer_id TEXT;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS stripe_subscription_id TEXT;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS subscription_status TEXT DEFAULT 'active';  -- active, canceling, past_due, canceled
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS current_period_start TIMESTAMPTZ;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS current_period_end TIMESTAMPTZ;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS rollover_credits BIGINT DEFAULT 0;

-- 4. Add ai_mode setting to user_profiles
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS ai_mode TEXT DEFAULT 'fast';  -- fast, good, thinking

-- 5. Set welcome bonus for new free users (100 credits)
-- Update the signup trigger
CREATE OR REPLACE FUNCTION public.create_user_profile()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.user_profiles (id, credit_balance, subscription_plan)
    VALUES (NEW.id, 100, 'free');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- 6. Function to reset credits on subscription renewal
-- Free plan: no rollover, credits simply reset to plan allocation
-- Paid plans: unused credits roll over, capped at 2× monthly allocation
CREATE OR REPLACE FUNCTION reset_subscription_credits(
    p_user_id UUID,
    p_plan_id TEXT,
    p_period_start TIMESTAMPTZ,
    p_period_end TIMESTAMPTZ
) RETURNS BIGINT AS $$
DECLARE
    v_plan_credits BIGINT;
    v_current_balance BIGINT;
    v_rollover BIGINT;
    v_new_balance BIGINT;
BEGIN
    -- Get plan credit allocation
    SELECT monthly_credits INTO v_plan_credits
    FROM subscription_plans WHERE id = p_plan_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Invalid plan: %', p_plan_id;
    END IF;

    -- Get current balance
    SELECT credit_balance INTO v_current_balance
    FROM user_profiles WHERE id = p_user_id;

    -- Free plan: no rollover, just reset to plan allocation
    -- Paid plans: rollover unused credits, capped at 2× monthly allocation
    IF p_plan_id = 'free' THEN
        v_rollover := 0;
        v_new_balance := v_plan_credits;
    ELSE
        v_rollover := LEAST(v_current_balance, v_plan_credits * 2);
        v_new_balance := v_plan_credits + v_rollover;
    END IF;

    -- Update user profile
    UPDATE user_profiles
    SET credit_balance = v_new_balance,
        rollover_credits = v_rollover,
        subscription_plan = p_plan_id,
        current_period_start = p_period_start,
        current_period_end = p_period_end,
        updated_at = now()
    WHERE id = p_user_id;

    -- Log the credit reset
    INSERT INTO credit_transactions (user_id, type, amount, balance_after, description)
    VALUES (p_user_id, 'subscription', v_plan_credits, v_new_balance,
            p_plan_id || ' monthly credits (' || v_rollover || ' rolled over)');

    RETURN v_new_balance;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 7. Update initial schema default
ALTER TABLE user_profiles ALTER COLUMN credit_balance SET DEFAULT 100;

-- 8. Convert existing users: set them to free plan with their current balance
UPDATE user_profiles
SET subscription_plan = 'free'
WHERE subscription_plan IS NULL;
