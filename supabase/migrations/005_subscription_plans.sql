-- Migrate from one-time credit packs to subscription plans
-- Each subscription gives a monthly credit allocation
-- Free plan: no rollover, credits simply reset each cycle
-- Paid plans: unused credits roll over, capped at 2× monthly allocation
-- Downgrade/cancel: paid credits expire, balance resets to free allocation

-- 1. subscription_plans table already created in 000_initial_schema.sql
-- Just ensure columns exist on user_profiles (idempotent)
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS subscription_plan TEXT DEFAULT 'free';
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS stripe_customer_id TEXT;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS stripe_subscription_id TEXT;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS subscription_status TEXT DEFAULT 'active';
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS current_period_start TIMESTAMPTZ;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS current_period_end TIMESTAMPTZ;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS rollover_credits BIGINT DEFAULT 0;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS ai_mode TEXT DEFAULT 'fast';

-- 2. Set welcome bonus for new free users (100 credits)
CREATE OR REPLACE FUNCTION public.create_user_profile()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.user_profiles (id, credit_balance, subscription_plan)
    VALUES (NEW.id, 100, 'free');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- 3. Function to reset credits on subscription renewal
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
    SELECT monthly_credits INTO v_plan_credits
    FROM subscription_plans WHERE id = p_plan_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Invalid plan: %', p_plan_id;
    END IF;

    SELECT credit_balance INTO v_current_balance
    FROM user_profiles WHERE id = p_user_id;

    IF p_plan_id = 'free' THEN
        v_rollover := 0;
        v_new_balance := v_plan_credits;
    ELSE
        v_rollover := LEAST(v_current_balance, v_plan_credits * 2);
        v_new_balance := v_plan_credits + v_rollover;
    END IF;

    UPDATE user_profiles
    SET credit_balance = v_new_balance,
        rollover_credits = v_rollover,
        subscription_plan = p_plan_id,
        current_period_start = p_period_start,
        current_period_end = p_period_end,
        updated_at = now()
    WHERE id = p_user_id;

    INSERT INTO credit_transactions (user_id, type, amount, balance_after, description)
    VALUES (p_user_id, 'subscription', v_plan_credits, v_new_balance,
            p_plan_id || ' monthly credits (' || v_rollover || ' rolled over)');

    RETURN v_new_balance;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 4. Update initial schema default
ALTER TABLE user_profiles ALTER COLUMN credit_balance SET DEFAULT 100;

-- 5. Convert existing users: set them to free plan with their current balance
UPDATE user_profiles
SET subscription_plan = 'free'
WHERE subscription_plan IS NULL;
