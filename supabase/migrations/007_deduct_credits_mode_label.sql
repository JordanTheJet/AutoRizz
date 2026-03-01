-- Update deduct_credits to log AI mode name instead of model name in transaction descriptions
-- Adds p_ai_mode parameter, uses INITCAP(mode) in description (e.g. "Fast mode", "Thinking mode")

CREATE OR REPLACE FUNCTION deduct_credits(
    p_user_id UUID,
    p_input_tokens BIGINT,
    p_output_tokens BIGINT,
    p_model TEXT,
    p_multiplier INT DEFAULT 1,
    p_ai_mode TEXT DEFAULT 'fast'
) RETURNS BIGINT AS $$
DECLARE
    v_cost BIGINT;
    v_balance BIGINT;
    v_mode_label TEXT;
BEGIN
    -- Calculate cost using explicit multiplier
    -- Formula: (input_tokens * multiplier + output_tokens * 3 * multiplier) / 1000
    v_cost := CEIL(
        ((p_input_tokens * p_multiplier) + (p_output_tokens * 3 * p_multiplier))::NUMERIC / 1000
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

    -- Friendly mode label for transaction description
    v_mode_label := INITCAP(p_ai_mode);

    -- Log the transaction
    INSERT INTO credit_transactions (user_id, type, amount, balance_after, description)
    VALUES (p_user_id, 'usage', -v_cost, v_balance, v_mode_label || ' mode');

    -- Log usage (keep model for internal tracking)
    INSERT INTO usage_logs (user_id, provider, model, input_tokens, output_tokens)
    VALUES (p_user_id, 'google', p_model, p_input_tokens, p_output_tokens);

    RETURN v_balance;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
