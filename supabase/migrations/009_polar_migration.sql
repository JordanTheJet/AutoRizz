-- Migrate from Stripe to Polar payments
-- Rename all stripe-specific columns to polar equivalents

-- user_profiles: stripe_customer_id → polar_customer_id, stripe_subscription_id → polar_subscription_id
ALTER TABLE user_profiles RENAME COLUMN stripe_customer_id TO polar_customer_id;
ALTER TABLE user_profiles RENAME COLUMN stripe_subscription_id TO polar_subscription_id;

-- credit_transactions: stripe_payment_id → polar_order_id
ALTER TABLE credit_transactions RENAME COLUMN stripe_payment_id TO polar_order_id;

-- subscription_plans: stripe_price_id → polar_product_id
ALTER TABLE subscription_plans RENAME COLUMN stripe_price_id TO polar_product_id;

-- Update subscription_plans with Polar product IDs
UPDATE subscription_plans SET polar_product_id = 'd6ca4d91-9857-41ba-bb9f-036c305ca35e' WHERE id = 'starter';
UPDATE subscription_plans SET polar_product_id = 'b1d359a5-1af9-43df-860d-328d9633e6c3' WHERE id = 'pro';
UPDATE subscription_plans SET polar_product_id = '31f47752-8f15-4c37-9b98-2b223bbd5569' WHERE id = 'ultra';

-- credit_packs: stripe_price_id → polar_product_id
ALTER TABLE credit_packs RENAME COLUMN stripe_price_id TO polar_product_id;
