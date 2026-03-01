-- Atomic credit deduction function
-- Called by the LLM proxy after each request completes
-- 1 credit ≈ $0.01 of AI usage
CREATE OR REPLACE FUNCTION deduct_credits(
    p_user_id UUID,
    p_input_tokens BIGINT,
    p_output_tokens BIGINT,
    p_model TEXT
) RETURNS BIGINT AS $$
DECLARE
    v_multiplier INT;
    v_cost BIGINT;
    v_balance BIGINT;
    v_provider TEXT;
BEGIN
    -- Determine model tier multiplier
    v_multiplier := CASE
        WHEN p_model ILIKE '%haiku%' OR p_model ILIKE '%mini%' OR p_model ILIKE '%flash%' THEN 1
        WHEN p_model ILIKE '%opus%' OR p_model ILIKE '%gpt-5.2%' OR p_model = 'gpt-5' THEN 15
        ELSE 4  -- Advanced tier (sonnet, gpt-4.1, gemini-pro, etc.)
    END;

    -- Calculate cost in credits (/ 1000 from raw token math)
    -- Formula: ceil((input × multiplier + output × 3 × multiplier) / 1000)
    v_cost := CEIL(
        ((p_input_tokens * v_multiplier) + (p_output_tokens * 3 * v_multiplier))::NUMERIC / 1000
    );

    -- Minimum 1 credit per request
    IF v_cost < 1 THEN v_cost := 1; END IF;

    -- Atomic deduction: only succeeds if balance >= cost
    UPDATE user_profiles
    SET credit_balance = credit_balance - v_cost,
        updated_at = now()
    WHERE id = p_user_id AND credit_balance >= v_cost
    RETURNING credit_balance INTO v_balance;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Insufficient credits';
    END IF;

    -- Log the credit transaction
    INSERT INTO credit_transactions (user_id, type, amount, balance_after, description)
    VALUES (p_user_id, 'usage', -v_cost, v_balance, p_model || ' conversation');

    -- Determine provider from model name
    v_provider := CASE
        WHEN p_model ILIKE 'claude%' THEN 'anthropic'
        WHEN p_model ILIKE 'gpt%' THEN 'openai'
        WHEN p_model ILIKE 'gemini%' THEN 'google'
        ELSE 'openrouter'
    END;

    -- Log usage (raw tokens for analytics)
    INSERT INTO usage_logs (user_id, provider, model, input_tokens, output_tokens)
    VALUES (p_user_id, v_provider, p_model, p_input_tokens, p_output_tokens);

    RETURN v_balance;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Add credits function (for bonuses, referrals, manual adjustments)
CREATE OR REPLACE FUNCTION add_credits(
    p_user_id UUID,
    p_amount BIGINT,
    p_type TEXT,
    p_description TEXT,
    p_stripe_payment_id TEXT DEFAULT NULL
) RETURNS BIGINT AS $$
DECLARE
    v_balance BIGINT;
BEGIN
    UPDATE user_profiles
    SET credit_balance = credit_balance + p_amount,
        total_credits_purchased = CASE WHEN p_type = 'purchase' THEN total_credits_purchased + p_amount ELSE total_credits_purchased END,
        updated_at = now()
    WHERE id = p_user_id
    RETURNING credit_balance INTO v_balance;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'User not found';
    END IF;

    INSERT INTO credit_transactions (user_id, type, amount, balance_after, description, stripe_payment_id)
    VALUES (p_user_id, p_type, p_amount, v_balance, p_description, p_stripe_payment_id);

    RETURN v_balance;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Reset subscription credits (called by webhook on invoice.paid each billing cycle)
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
