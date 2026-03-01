-- Simplify credit system: divide all values by 1000
-- Old: 1 credit = 1 token at standard tier (confusing large numbers)
-- New: 1 credit ≈ $0.01 of AI usage (human-readable numbers)
--
-- Conversion: all credit balances and pack amounts / 1000
-- Server-side deduct_credits now divides calculated cost by 1000

-- 1. Update the deduct_credits function to use new scale
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

    -- Calculate cost in new credit units (/ 1000 from old system)
    -- Formula: (input_tokens * multiplier + output_tokens * 3 * multiplier) / 1000
    -- Round up so we never undercharge
    v_cost := CEIL(
        ((p_input_tokens * v_multiplier) + (p_output_tokens * 3 * v_multiplier))::NUMERIC / 1000
    );

    -- Minimum 1 credit per request
    IF v_cost < 1 THEN v_cost := 1; END IF;

    -- Atomic deduction
    UPDATE user_profiles
    SET credit_balance = credit_balance - v_cost,
        updated_at = now()
    WHERE id = p_user_id AND credit_balance >= v_cost
    RETURNING credit_balance INTO v_balance;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Insufficient credits';
    END IF;

    -- Log the transaction
    INSERT INTO credit_transactions (user_id, type, amount, balance_after, description)
    VALUES (p_user_id, 'usage', -v_cost, v_balance, p_model || ' conversation');

    -- Determine provider
    v_provider := CASE
        WHEN p_model ILIKE 'claude%' THEN 'anthropic'
        WHEN p_model ILIKE 'gpt%' THEN 'openai'
        WHEN p_model ILIKE 'gemini%' THEN 'google'
        ELSE 'openrouter'
    END;

    -- Log usage (still tracks raw tokens for analytics)
    INSERT INTO usage_logs (user_id, provider, model, input_tokens, output_tokens)
    VALUES (p_user_id, v_provider, p_model, p_input_tokens, p_output_tokens);

    RETURN v_balance;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 2. Convert existing user balances (divide by 1000, round up)
UPDATE user_profiles
SET credit_balance = CEIL(credit_balance::NUMERIC / 1000);

-- 3. Convert existing transaction amounts
UPDATE credit_transactions
SET amount = CASE
    WHEN amount >= 0 THEN CEIL(amount::NUMERIC / 1000)
    ELSE -CEIL(ABS(amount)::NUMERIC / 1000)
END,
balance_after = CEIL(balance_after::NUMERIC / 1000);

-- 4. Update total_credits_purchased
UPDATE user_profiles
SET total_credits_purchased = CEIL(total_credits_purchased::NUMERIC / 1000);

-- 5. Update credit packs to new values
UPDATE credit_packs SET credits = 500 WHERE id = 'starter';
UPDATE credit_packs SET credits = 2000 WHERE id = 'builder';
UPDATE credit_packs SET credits = 6000 WHERE id = 'power';

-- 6. Update the signup trigger to give 50 credits (was 50000)
CREATE OR REPLACE FUNCTION public.create_user_profile()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.user_profiles (id, credit_balance)
    VALUES (NEW.id, 50);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- 7. Update auto_refill_threshold defaults
UPDATE user_profiles
SET auto_refill_threshold = CEIL(auto_refill_threshold::NUMERIC / 1000)
WHERE auto_refill_threshold > 0;
