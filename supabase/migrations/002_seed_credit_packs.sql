-- Seed credit pack catalog
-- 1 credit ≈ $0.01 of AI usage
INSERT INTO credit_packs (id, name, credits, price_usd, stripe_price_id) VALUES
    ('starter', 'Starter', 500, 4.99, 'price_1T5pogQWWPsMEhJUV9JJbO0O'),
    ('builder', 'Builder', 2000, 14.99, 'price_1T5pohQWWPsMEhJUK3Lt6Px6'),
    ('power', 'Power', 6000, 39.99, 'price_1T5pohQWWPsMEhJUUFnuXKXD');
