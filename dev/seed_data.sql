-- Seed data for local development.
-- Usage: psql -h localhost -U cookmaid -d cookmaid -v user_id="'<your-user-uuid>'" -f seed_data.sql
--
-- To find your user_id after first login:
--   SELECT id FROM users LIMIT 1;

-- Clean existing data (preserves schema and users)
TRUNCATE meal_plan_items, recipe_ingredients, shopping_items, shopping_lists, recipes CASCADE;

-- Shorthand for catalog item UUIDs: CI(N) = 00000000-0000-0000-0001-00000000NNNN
-- Shorthand for recipe UUIDs:       R(N)  = 00000000-0000-0000-2000-00000000NNNN

-- ============================================================
-- Shopping Lists
-- ============================================================

INSERT INTO shopping_lists (id, user_id, name, is_default) VALUES
    ('00000000-0000-0000-1000-000000000001', :user_id, 'Weekly Groceries', FALSE);

INSERT INTO shopping_items (list_id, catalog_item_id, free_text_name, quantity, checked) VALUES
    ((SELECT id FROM shopping_lists WHERE user_id = :user_id AND is_default = TRUE),
        '00000000-0000-0000-0001-000000000046', NULL, 1, FALSE),    -- Milk
    ((SELECT id FROM shopping_lists WHERE user_id = :user_id AND is_default = TRUE),
        '00000000-0000-0000-0001-000000000049', NULL, 6, FALSE),    -- Eggs
    ((SELECT id FROM shopping_lists WHERE user_id = :user_id AND is_default = TRUE),
        '00000000-0000-0000-0001-000000000001', NULL, 4, FALSE),    -- Apples
    ((SELECT id FROM shopping_lists WHERE user_id = :user_id AND is_default = TRUE),
        '00000000-0000-0000-0001-000000000022', NULL, NULL, FALSE),  -- Tomatoes
    ((SELECT id FROM shopping_lists WHERE user_id = :user_id AND is_default = TRUE),
        '00000000-0000-0000-0001-000000000085', NULL, 1, FALSE),    -- Bread
    ((SELECT id FROM shopping_lists WHERE user_id = :user_id AND is_default = TRUE),
        NULL, 'Kitchen sponges', 2, FALSE),
    ((SELECT id FROM shopping_lists WHERE user_id = :user_id AND is_default = TRUE),
        '00000000-0000-0000-0001-000000000047', NULL, NULL, TRUE),  -- Butter (checked)
    ((SELECT id FROM shopping_lists WHERE user_id = :user_id AND is_default = TRUE),
        '00000000-0000-0000-0001-000000000106', NULL, NULL, TRUE);  -- Coffee (checked)

INSERT INTO shopping_items (list_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-1000-000000000001', 'Birthday cake', 1),
    ('00000000-0000-0000-1000-000000000001', 'Candles', NULL),
    ('00000000-0000-0000-1000-000000000001', 'Party plates', 20);

-- ============================================================
-- Recipes (40 total)
-- ============================================================

-- 1: Spaghetti Bolognese
INSERT INTO recipes (id, user_id, name, description, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000001', :user_id, 'Spaghetti Bolognese',
     'Classic Italian pasta dish with rich meat sauce.',
     ARRAY['Cook spaghetti according to package', 'Brown ground beef with onions and garlic', 'Add canned tomatoes, tomato paste, and herbs', 'Simmer 20 minutes', 'Serve over pasta with parmesan'],
     ARRAY['Pasta', 'Italian', 'Dinner'], 4);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000001', '00000000-0000-0000-0001-000000000096', NULL, '500g'),
    ('00000000-0000-0000-2000-000000000001', '00000000-0000-0000-0001-000000000063', NULL, '400g'),
    ('00000000-0000-0000-2000-000000000001', '00000000-0000-0000-0001-000000000021', NULL, '2 medium'),
    ('00000000-0000-0000-2000-000000000001', '00000000-0000-0000-0001-000000000025', NULL, '3 cloves'),
    ('00000000-0000-0000-2000-000000000001', '00000000-0000-0000-0001-000000000133', NULL, '1 can'),
    ('00000000-0000-0000-2000-000000000001', '00000000-0000-0000-0001-000000000134', NULL, NULL),
    ('00000000-0000-0000-2000-000000000001', '00000000-0000-0000-0001-000000000055', NULL, NULL),
    ('00000000-0000-0000-2000-000000000001', '00000000-0000-0000-0001-000000000173', NULL, NULL);

-- 2: Chicken Stir Fry
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000002', :user_id, 'Chicken Stir Fry',
     ARRAY['Cut chicken into strips', 'Stir fry vegetables in sesame oil', 'Add chicken and cook through', 'Add soy sauce and serve over rice'],
     ARRAY['Asian', 'Quick', 'Dinner'], 3);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000002', '00000000-0000-0000-0001-000000000061', NULL, '500g'),
    ('00000000-0000-0000-2000-000000000002', '00000000-0000-0000-0001-000000000027', NULL, '2 medium'),
    ('00000000-0000-0000-2000-000000000002', '00000000-0000-0000-0001-000000000028', NULL, '1 head'),
    ('00000000-0000-0000-2000-000000000002', '00000000-0000-0000-0001-000000000095', NULL, '1 1/2 cups'),
    ('00000000-0000-0000-2000-000000000002', '00000000-0000-0000-0001-000000000146', NULL, NULL),
    ('00000000-0000-0000-2000-000000000002', '00000000-0000-0000-0001-000000000176', NULL, NULL);

-- 3: Greek Salad
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000003', :user_id, 'Greek Salad',
     ARRAY['Chop cucumber, tomatoes, and onion', 'Add olives and feta', 'Dress with olive oil and oregano'],
     ARRAY['Salad', 'Quick', 'Lunch', 'Mediterranean'], 2);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000003', '00000000-0000-0000-0001-000000000031', NULL, '1 large'),
    ('00000000-0000-0000-2000-000000000003', '00000000-0000-0000-0001-000000000022', NULL, '3 medium'),
    ('00000000-0000-0000-2000-000000000003', '00000000-0000-0000-0001-000000000021', NULL, '1 small'),
    ('00000000-0000-0000-2000-000000000003', '00000000-0000-0000-0001-000000000057', NULL, '100g'),
    ('00000000-0000-0000-2000-000000000003', '00000000-0000-0000-0001-000000000139', NULL, NULL),
    ('00000000-0000-0000-2000-000000000003', '00000000-0000-0000-0001-000000000173', NULL, NULL),
    ('00000000-0000-0000-2000-000000000003', '00000000-0000-0000-0001-000000000159', NULL, NULL);

-- 4: Banana Pancakes
INSERT INTO recipes (id, user_id, name, description, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000004', :user_id, 'Banana Pancakes',
     'Fluffy pancakes with mashed banana. Great weekend breakfast.',
     ARRAY['Mash bananas', 'Mix in eggs, flour, and baking powder', 'Cook on buttered pan until golden'],
     ARRAY['Breakfast', 'Sweet'], 2);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000004', '00000000-0000-0000-0001-000000000002', NULL, '2 ripe'),
    ('00000000-0000-0000-2000-000000000004', '00000000-0000-0000-0001-000000000049', NULL, '2'),
    ('00000000-0000-0000-2000-000000000004', '00000000-0000-0000-0001-000000000291', NULL, '1 cup'),
    ('00000000-0000-0000-2000-000000000004', '00000000-0000-0000-0001-000000000184', NULL, NULL),
    ('00000000-0000-0000-2000-000000000004', '00000000-0000-0000-0001-000000000047', NULL, NULL);

-- 5: Tomato Soup
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000005', :user_id, 'Tomato Soup',
     ARRAY['Sauté onions and garlic in olive oil', 'Add canned tomatoes and broth', 'Simmer 15 minutes', 'Blend until smooth', 'Season with salt and pepper'],
     ARRAY['Soup', 'Quick', 'Lunch'], 4);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000005', '00000000-0000-0000-0001-000000000133', NULL, '2 cans'),
    ('00000000-0000-0000-2000-000000000005', '00000000-0000-0000-0001-000000000021', NULL, '1 medium'),
    ('00000000-0000-0000-2000-000000000005', '00000000-0000-0000-0001-000000000025', NULL, '2 cloves'),
    ('00000000-0000-0000-2000-000000000005', '00000000-0000-0000-0001-000000000173', NULL, NULL),
    ('00000000-0000-0000-2000-000000000005', '00000000-0000-0000-0001-000000000051', NULL, NULL),
    ('00000000-0000-0000-2000-000000000005', NULL, 'Vegetable broth', '500ml');

-- 6: Overnight Oats
INSERT INTO recipes (id, user_id, name, description, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000006', :user_id, 'Overnight Oats',
     'https://example.com/overnight-oats-recipe',
     ARRAY['Mix oats with milk and yogurt', 'Add chia seeds and honey', 'Refrigerate overnight', 'Top with berries'],
     ARRAY['Breakfast', 'Meal Prep'], 1);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000006', '00000000-0000-0000-0001-000000000102', NULL, '1/2 cup'),
    ('00000000-0000-0000-2000-000000000006', '00000000-0000-0000-0001-000000000046', NULL, '200ml'),
    ('00000000-0000-0000-2000-000000000006', '00000000-0000-0000-0001-000000000050', NULL, '100g'),
    ('00000000-0000-0000-2000-000000000006', '00000000-0000-0000-0001-000000000196', NULL, NULL),
    ('00000000-0000-0000-2000-000000000006', '00000000-0000-0000-0001-000000000008', NULL, NULL),
    ('00000000-0000-0000-2000-000000000006', '00000000-0000-0000-0001-000000000223', NULL, NULL);

-- 7: Penne Arrabbiata
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000007', :user_id, 'Penne Arrabbiata',
     ARRAY['Cook penne', 'Sauté garlic and chili flakes in olive oil', 'Add canned tomatoes', 'Toss with pasta and basil'],
     ARRAY['Pasta', 'Italian', 'Vegetarian', 'Quick'], NULL);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000007', '00000000-0000-0000-0001-000000000097', NULL, '400g'),
    ('00000000-0000-0000-2000-000000000007', '00000000-0000-0000-0001-000000000025', NULL, '4 cloves'),
    ('00000000-0000-0000-2000-000000000007', '00000000-0000-0000-0001-000000000133', NULL, '1 can'),
    ('00000000-0000-0000-2000-000000000007', '00000000-0000-0000-0001-000000000173', NULL, NULL),
    ('00000000-0000-0000-2000-000000000007', '00000000-0000-0000-0001-000000000160', NULL, NULL),
    ('00000000-0000-0000-2000-000000000007', NULL, 'Chili flakes', NULL);

-- 8: Salmon with Asparagus
INSERT INTO recipes (id, user_id, name, description, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000008', :user_id, 'Salmon with Asparagus',
     'Simple sheet pan dinner ready in 25 minutes.',
     ARRAY['Preheat oven to 200°C', 'Season salmon and asparagus with olive oil, salt, pepper', 'Bake 15-18 minutes', 'Squeeze lemon on top'],
     ARRAY['Dinner', 'Healthy', 'Quick'], 2);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000008', '00000000-0000-0000-0001-000000000075', NULL, '400g'),
    ('00000000-0000-0000-2000-000000000008', '00000000-0000-0000-0001-000000000041', NULL, '1 bunch'),
    ('00000000-0000-0000-2000-000000000008', '00000000-0000-0000-0001-000000000004', NULL, '1'),
    ('00000000-0000-0000-2000-000000000008', '00000000-0000-0000-0001-000000000173', NULL, NULL);

-- 9: Chicken Tacos
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000009', :user_id, 'Chicken Tacos',
     ARRAY['Season chicken with cumin and paprika', 'Grill or pan-fry chicken', 'Warm tortillas', 'Assemble with toppings'],
     ARRAY['Mexican', 'Dinner', 'Quick'], 4);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000009', '00000000-0000-0000-0001-000000000061', NULL, '500g'),
    ('00000000-0000-0000-2000-000000000009', '00000000-0000-0000-0001-000000000087', NULL, '8'),
    ('00000000-0000-0000-2000-000000000009', '00000000-0000-0000-0001-000000000158', NULL, NULL),
    ('00000000-0000-0000-2000-000000000009', '00000000-0000-0000-0001-000000000157', NULL, NULL),
    ('00000000-0000-0000-2000-000000000009', '00000000-0000-0000-0001-000000000005', NULL, '2'),
    ('00000000-0000-0000-2000-000000000009', NULL, 'Salsa', NULL),
    ('00000000-0000-0000-2000-000000000009', '00000000-0000-0000-0001-000000000052', NULL, NULL);

-- 10: Mushroom Risotto
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000010', :user_id, 'Mushroom Risotto',
     ARRAY['Sauté mushrooms and set aside', 'Toast rice in butter with onions', 'Add broth one ladle at a time, stirring', 'Fold in mushrooms and parmesan'],
     ARRAY['Italian', 'Dinner', 'Vegetarian', 'Comfort Food'], 3);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000010', '00000000-0000-0000-0001-000000000033', NULL, '300g'),
    ('00000000-0000-0000-2000-000000000010', '00000000-0000-0000-0001-000000000095', NULL, '1 1/2 cups'),
    ('00000000-0000-0000-2000-000000000010', '00000000-0000-0000-0001-000000000021', NULL, '1 medium'),
    ('00000000-0000-0000-2000-000000000010', '00000000-0000-0000-0001-000000000047', NULL, NULL),
    ('00000000-0000-0000-2000-000000000010', '00000000-0000-0000-0001-000000000055', NULL, NULL),
    ('00000000-0000-0000-2000-000000000010', NULL, 'Chicken broth', '750ml');

-- 11: Avocado Toast
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000011', :user_id, 'Avocado Toast',
     ARRAY['Toast bread', 'Mash avocado with lime and salt', 'Spread on toast', 'Top with egg if desired'],
     ARRAY['Breakfast', 'Quick', 'Vegetarian'], 1);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000011', '00000000-0000-0000-0001-000000000085', NULL, '2 slices'),
    ('00000000-0000-0000-2000-000000000011', '00000000-0000-0000-0001-000000000013', NULL, '1'),
    ('00000000-0000-0000-2000-000000000011', '00000000-0000-0000-0001-000000000005', NULL, '1/2'),
    ('00000000-0000-0000-2000-000000000011', '00000000-0000-0000-0001-000000000155', NULL, NULL);

-- 12: Beef Tacos
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000012', :user_id, 'Beef Tacos',
     ARRAY['Brown ground beef with onions', 'Season with cumin, paprika, salt', 'Warm tortillas', 'Top with cheese and sour cream'],
     ARRAY['Mexican', 'Dinner'], 4);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000012', '00000000-0000-0000-0001-000000000063', NULL, '500g'),
    ('00000000-0000-0000-2000-000000000012', '00000000-0000-0000-0001-000000000087', NULL, '8'),
    ('00000000-0000-0000-2000-000000000012', '00000000-0000-0000-0001-000000000021', NULL, '1 medium'),
    ('00000000-0000-0000-2000-000000000012', '00000000-0000-0000-0001-000000000048', NULL, NULL),
    ('00000000-0000-0000-2000-000000000012', '00000000-0000-0000-0001-000000000052', NULL, NULL);

-- 13: Shakshuka
INSERT INTO recipes (id, user_id, name, description, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000013', :user_id, 'Shakshuka',
     'North African egg dish in spiced tomato sauce.',
     ARRAY['Sauté onions and bell peppers', 'Add canned tomatoes, cumin, paprika', 'Make wells and crack in eggs', 'Cover and cook until eggs set'],
     ARRAY['Breakfast', 'Mediterranean', 'Vegetarian'], 2);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000013', '00000000-0000-0000-0001-000000000049', NULL, '4'),
    ('00000000-0000-0000-2000-000000000013', '00000000-0000-0000-0001-000000000133', NULL, '1 can'),
    ('00000000-0000-0000-2000-000000000013', '00000000-0000-0000-0001-000000000027', NULL, '1 large'),
    ('00000000-0000-0000-2000-000000000013', '00000000-0000-0000-0001-000000000021', NULL, '1 medium'),
    ('00000000-0000-0000-2000-000000000013', '00000000-0000-0000-0001-000000000158', NULL, NULL),
    ('00000000-0000-0000-2000-000000000013', '00000000-0000-0000-0001-000000000157', NULL, NULL);

-- 14: Pad Thai
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000014', :user_id, 'Pad Thai',
     ARRAY['Soak rice noodles', 'Stir fry shrimp or tofu', 'Add noodles, eggs, and sauce', 'Top with peanuts and lime'],
     ARRAY['Asian', 'Dinner'], 3);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000014', '00000000-0000-0000-0001-000000000099', NULL, '250g'),
    ('00000000-0000-0000-2000-000000000014', '00000000-0000-0000-0001-000000000076', NULL, '300g'),
    ('00000000-0000-0000-2000-000000000014', '00000000-0000-0000-0001-000000000049', NULL, '2'),
    ('00000000-0000-0000-2000-000000000014', '00000000-0000-0000-0001-000000000005', NULL, '2'),
    ('00000000-0000-0000-2000-000000000014', NULL, 'Fish sauce', NULL),
    ('00000000-0000-0000-2000-000000000014', NULL, 'Peanuts', NULL);

-- 15: Caprese Salad
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000015', :user_id, 'Caprese Salad',
     ARRAY['Slice tomatoes and mozzarella', 'Layer on plate', 'Add basil leaves', 'Drizzle with olive oil and balsamic'],
     ARRAY['Salad', 'Italian', 'Quick', 'Vegetarian'], 2);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000015', '00000000-0000-0000-0001-000000000022', NULL, '3 large'),
    ('00000000-0000-0000-2000-000000000015', NULL, 'Fresh mozzarella', '250g'),
    ('00000000-0000-0000-2000-000000000015', '00000000-0000-0000-0001-000000000160', NULL, NULL),
    ('00000000-0000-0000-2000-000000000015', '00000000-0000-0000-0001-000000000173', NULL, NULL);

-- 16: Chicken Curry
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000016', :user_id, 'Chicken Curry',
     ARRAY['Sauté onions, garlic, ginger', 'Add spices and cook 1 minute', 'Add chicken and coconut milk', 'Simmer 20 minutes', 'Serve with rice'],
     ARRAY['Indian', 'Dinner', 'Comfort Food'], 4);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000016', '00000000-0000-0000-0001-000000000061', NULL, '600g'),
    ('00000000-0000-0000-2000-000000000016', '00000000-0000-0000-0001-000000000141', NULL, '1 can'),
    ('00000000-0000-0000-2000-000000000016', '00000000-0000-0000-0001-000000000021', NULL, '2 medium'),
    ('00000000-0000-0000-2000-000000000016', '00000000-0000-0000-0001-000000000025', NULL, '3 cloves'),
    ('00000000-0000-0000-2000-000000000016', '00000000-0000-0000-0001-000000000026', NULL, NULL),
    ('00000000-0000-0000-2000-000000000016', '00000000-0000-0000-0001-000000000095', NULL, '1 1/2 cups'),
    ('00000000-0000-0000-2000-000000000016', NULL, 'Curry powder', NULL);

-- 17: Caesar Salad
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000017', :user_id, 'Caesar Salad',
     ARRAY['Tear lettuce into pieces', 'Make dressing with egg, lemon, garlic, parmesan', 'Toss and add croutons'],
     ARRAY['Salad', 'Lunch'], NULL);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000017', '00000000-0000-0000-0001-000000000030', NULL, '1 head'),
    ('00000000-0000-0000-2000-000000000017', '00000000-0000-0000-0001-000000000055', NULL, NULL),
    ('00000000-0000-0000-2000-000000000017', '00000000-0000-0000-0001-000000000004', NULL, '1'),
    ('00000000-0000-0000-2000-000000000017', NULL, 'Croutons', NULL);

-- 18: Sweet Potato Soup
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000018', :user_id, 'Sweet Potato Soup',
     ARRAY['Peel and cube sweet potatoes', 'Sauté with onions and ginger', 'Add broth and simmer until tender', 'Blend smooth and season'],
     ARRAY['Soup', 'Vegetarian', 'Healthy'], 4);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000018', '00000000-0000-0000-0001-000000000040', NULL, '600g'),
    ('00000000-0000-0000-2000-000000000018', '00000000-0000-0000-0001-000000000021', NULL, '1 medium'),
    ('00000000-0000-0000-2000-000000000018', '00000000-0000-0000-0001-000000000026', NULL, NULL),
    ('00000000-0000-0000-2000-000000000018', NULL, 'Vegetable broth', '750ml');

-- 19: Fish and Chips
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000019', :user_id, 'Fish and Chips',
     ARRAY['Make batter with flour and beer', 'Cut potatoes into chips and fry', 'Coat cod in batter and deep fry', 'Serve with lemon'],
     ARRAY['Dinner', 'Comfort Food'], 4);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000019', '00000000-0000-0000-0001-000000000078', NULL, '500g'),
    ('00000000-0000-0000-2000-000000000019', '00000000-0000-0000-0001-000000000023', NULL, '800g'),
    ('00000000-0000-0000-2000-000000000019', '00000000-0000-0000-0001-000000000291', NULL, '1 1/2 cups'),
    ('00000000-0000-0000-2000-000000000019', '00000000-0000-0000-0001-000000000004', NULL, '1'),
    ('00000000-0000-0000-2000-000000000019', NULL, 'Beer', '200ml');

-- 20: Quinoa Bowl
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000020', :user_id, 'Quinoa Bowl',
     ARRAY['Cook quinoa', 'Roast sweet potatoes and chickpeas', 'Assemble bowl with spinach', 'Drizzle with tahini dressing'],
     ARRAY['Lunch', 'Healthy', 'Vegetarian', 'Meal Prep'], 2);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000020', '00000000-0000-0000-0001-000000000101', NULL, '1 cup'),
    ('00000000-0000-0000-2000-000000000020', '00000000-0000-0000-0001-000000000040', NULL, '300g'),
    ('00000000-0000-0000-2000-000000000020', '00000000-0000-0000-0001-000000000136', NULL, '1 can'),
    ('00000000-0000-0000-2000-000000000020', '00000000-0000-0000-0001-000000000029', NULL, '2 cups'),
    ('00000000-0000-0000-2000-000000000020', NULL, 'Tahini', NULL);

-- 21: Scrambled Eggs
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000021', :user_id, 'Scrambled Eggs',
     ARRAY['Whisk eggs with salt and pepper', 'Melt butter in pan', 'Cook on low heat, stirring gently', 'Serve on toast'],
     ARRAY['Breakfast', 'Quick'], 1);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000021', '00000000-0000-0000-0001-000000000049', NULL, '4'),
    ('00000000-0000-0000-2000-000000000021', '00000000-0000-0000-0001-000000000047', NULL, NULL),
    ('00000000-0000-0000-2000-000000000021', '00000000-0000-0000-0001-000000000085', NULL, '2 slices');

-- 22: Peanut Butter Smoothie
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000022', :user_id, 'Peanut Butter Smoothie',
     ARRAY['Blend all ingredients until smooth', 'Add ice if desired'],
     ARRAY['Breakfast', 'Quick', 'Healthy'], 1);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000022', '00000000-0000-0000-0001-000000000002', NULL, '1'),
    ('00000000-0000-0000-2000-000000000022', '00000000-0000-0000-0001-000000000199', NULL, NULL),
    ('00000000-0000-0000-2000-000000000022', '00000000-0000-0000-0001-000000000046', NULL, '250ml'),
    ('00000000-0000-0000-2000-000000000022', '00000000-0000-0000-0001-000000000223', NULL, NULL);

-- 23: Minestrone
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000023', :user_id, 'Minestrone Soup',
     ARRAY['Sauté onions, carrots, celery', 'Add canned tomatoes, beans, and broth', 'Add pasta and simmer', 'Season with herbs'],
     ARRAY['Soup', 'Italian', 'Vegetarian'], 6);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000023', '00000000-0000-0000-0001-000000000021', NULL, '1 medium'),
    ('00000000-0000-0000-2000-000000000023', '00000000-0000-0000-0001-000000000024', NULL, '2 medium'),
    ('00000000-0000-0000-2000-000000000023', '00000000-0000-0000-0001-000000000034', NULL, '2 stalks'),
    ('00000000-0000-0000-2000-000000000023', '00000000-0000-0000-0001-000000000133', NULL, '1 can'),
    ('00000000-0000-0000-2000-000000000023', NULL, 'Canned beans', '1 can'),
    ('00000000-0000-0000-2000-000000000023', '00000000-0000-0000-0001-000000000097', NULL, '100g');

-- 24: Tofu Stir Fry
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000024', :user_id, 'Tofu Stir Fry',
     ARRAY['Press and cube tofu', 'Pan fry until golden', 'Stir fry vegetables', 'Combine with soy sauce and sesame oil'],
     ARRAY['Asian', 'Vegetarian', 'Dinner'], 2);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000024', '00000000-0000-0000-0001-000000000225', NULL, '400g'),
    ('00000000-0000-0000-2000-000000000024', '00000000-0000-0000-0001-000000000028', NULL, '1 head'),
    ('00000000-0000-0000-2000-000000000024', '00000000-0000-0000-0001-000000000027', NULL, '1 large'),
    ('00000000-0000-0000-2000-000000000024', '00000000-0000-0000-0001-000000000146', NULL, NULL),
    ('00000000-0000-0000-2000-000000000024', '00000000-0000-0000-0001-000000000176', NULL, NULL),
    ('00000000-0000-0000-2000-000000000024', '00000000-0000-0000-0001-000000000095', NULL, '1 1/2 cups');

-- 25: Hummus Pita
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000025', :user_id, 'Hummus Pita Plate',
     ARRAY['Warm pita bread', 'Serve with hummus, cucumber, tomatoes', 'Add olives and feta'],
     ARRAY['Lunch', 'Mediterranean', 'Quick', 'Vegetarian'], NULL);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000025', '00000000-0000-0000-0001-000000000088', NULL, '4'),
    ('00000000-0000-0000-2000-000000000025', NULL, 'Hummus', '200g'),
    ('00000000-0000-0000-2000-000000000025', '00000000-0000-0000-0001-000000000031', NULL, '1'),
    ('00000000-0000-0000-2000-000000000025', '00000000-0000-0000-0001-000000000022', NULL, '2 medium'),
    ('00000000-0000-0000-2000-000000000025', '00000000-0000-0000-0001-000000000057', NULL, NULL);

-- 26: Mashed Potatoes
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000026', :user_id, 'Mashed Potatoes',
     ARRAY['Peel and boil potatoes until tender', 'Drain and mash', 'Add butter, milk, salt and pepper', 'Whip until fluffy'],
     ARRAY['Dinner', 'Comfort Food'], 6);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000026', '00000000-0000-0000-0001-000000000023', NULL, '1kg'),
    ('00000000-0000-0000-2000-000000000026', '00000000-0000-0000-0001-000000000047', NULL, NULL),
    ('00000000-0000-0000-2000-000000000026', '00000000-0000-0000-0001-000000000046', NULL, '1/2 cup');

-- 27: Spinach Omelette
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000027', :user_id, 'Spinach Omelette',
     ARRAY['Whisk eggs', 'Sauté spinach and mushrooms', 'Pour eggs over vegetables', 'Fold and serve'],
     ARRAY['Breakfast', 'Quick', 'Healthy'], 1);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000027', '00000000-0000-0000-0001-000000000049', NULL, '3'),
    ('00000000-0000-0000-2000-000000000027', '00000000-0000-0000-0001-000000000029', NULL, '2 cups'),
    ('00000000-0000-0000-2000-000000000027', '00000000-0000-0000-0001-000000000033', NULL, NULL),
    ('00000000-0000-0000-2000-000000000027', '00000000-0000-0000-0001-000000000047', NULL, NULL);

-- 28: Cinnamon Oatmeal
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000028', :user_id, 'Cinnamon Oatmeal',
     ARRAY['Cook oats with milk', 'Stir in cinnamon and honey', 'Top with banana slices and almonds'],
     ARRAY['Breakfast', 'Healthy'], 1);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000028', '00000000-0000-0000-0001-000000000102', NULL, '1/2 cup'),
    ('00000000-0000-0000-2000-000000000028', '00000000-0000-0000-0001-000000000046', NULL, '200ml'),
    ('00000000-0000-0000-2000-000000000028', '00000000-0000-0000-0001-000000000163', NULL, NULL),
    ('00000000-0000-0000-2000-000000000028', '00000000-0000-0000-0001-000000000002', NULL, '1'),
    ('00000000-0000-0000-2000-000000000028', '00000000-0000-0000-0001-000000000191', NULL, NULL),
    ('00000000-0000-0000-2000-000000000028', '00000000-0000-0000-0001-000000000223', NULL, NULL);

-- 29: Shrimp Scampi
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000029', :user_id, 'Shrimp Scampi',
     ARRAY['Cook spaghetti', 'Sauté garlic in butter and olive oil', 'Add shrimp and cook 3 minutes', 'Toss with pasta, lemon, and parsley'],
     ARRAY['Pasta', 'Dinner', 'Quick'], 3);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000029', '00000000-0000-0000-0001-000000000076', NULL, '400g'),
    ('00000000-0000-0000-2000-000000000029', '00000000-0000-0000-0001-000000000096', NULL, '400g'),
    ('00000000-0000-0000-2000-000000000029', '00000000-0000-0000-0001-000000000025', NULL, '4 cloves'),
    ('00000000-0000-0000-2000-000000000029', '00000000-0000-0000-0001-000000000047', NULL, NULL),
    ('00000000-0000-0000-2000-000000000029', '00000000-0000-0000-0001-000000000004', NULL, '1');

-- 30: Veggie Wrap
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000030', :user_id, 'Veggie Wrap',
     ARRAY['Spread hummus on tortilla', 'Add lettuce, tomato, cucumber, bell pepper', 'Roll tightly and cut in half'],
     ARRAY['Lunch', 'Vegetarian', 'Quick'], 2);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000030', '00000000-0000-0000-0001-000000000087', NULL, '2 large'),
    ('00000000-0000-0000-2000-000000000030', '00000000-0000-0000-0001-000000000030', NULL, NULL),
    ('00000000-0000-0000-2000-000000000030', '00000000-0000-0000-0001-000000000022', NULL, '1 medium'),
    ('00000000-0000-0000-2000-000000000030', '00000000-0000-0000-0001-000000000031', NULL, '1/2'),
    ('00000000-0000-0000-2000-000000000030', '00000000-0000-0000-0001-000000000027', NULL, '1'),
    ('00000000-0000-0000-2000-000000000030', NULL, 'Hummus', NULL);

-- 31: Garlic Bread
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000031', :user_id, 'Garlic Bread',
     ARRAY['Mix butter with minced garlic and parsley', 'Spread on bread halves', 'Bake at 180°C for 10 minutes'],
     ARRAY['Quick', 'Italian'], 4);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000031', '00000000-0000-0000-0001-000000000085', NULL, '1 loaf'),
    ('00000000-0000-0000-2000-000000000031', '00000000-0000-0000-0001-000000000047', NULL, NULL),
    ('00000000-0000-0000-2000-000000000031', '00000000-0000-0000-0001-000000000025', NULL, '4 cloves');

-- 32: Lentil Soup
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000032', :user_id, 'Lentil Soup',
     ARRAY['Sauté onions, carrots, garlic', 'Add lentils, cumin, and broth', 'Simmer 25 minutes', 'Season and serve with lemon'],
     ARRAY['Soup', 'Vegetarian', 'Healthy', 'Indian'], 4);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000032', NULL, 'Red lentils', '1 cup'),
    ('00000000-0000-0000-2000-000000000032', '00000000-0000-0000-0001-000000000021', NULL, '1 medium'),
    ('00000000-0000-0000-2000-000000000032', '00000000-0000-0000-0001-000000000024', NULL, '2 medium'),
    ('00000000-0000-0000-2000-000000000032', '00000000-0000-0000-0001-000000000025', NULL, '2 cloves'),
    ('00000000-0000-0000-2000-000000000032', '00000000-0000-0000-0001-000000000158', NULL, NULL),
    ('00000000-0000-0000-2000-000000000032', '00000000-0000-0000-0001-000000000004', NULL, '1');

-- 33: Fried Rice
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000033', :user_id, 'Fried Rice',
     ARRAY['Cook rice and let cool', 'Scramble eggs and set aside', 'Stir fry vegetables', 'Add rice, soy sauce, and sesame oil'],
     ARRAY['Asian', 'Quick', 'Dinner'], 3);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000033', '00000000-0000-0000-0001-000000000095', NULL, '2 cups'),
    ('00000000-0000-0000-2000-000000000033', '00000000-0000-0000-0001-000000000049', NULL, '2'),
    ('00000000-0000-0000-2000-000000000033', '00000000-0000-0000-0001-000000000024', NULL, '1 medium'),
    ('00000000-0000-0000-2000-000000000033', '00000000-0000-0000-0001-000000000036', NULL, NULL),
    ('00000000-0000-0000-2000-000000000033', '00000000-0000-0000-0001-000000000146', NULL, NULL),
    ('00000000-0000-0000-2000-000000000033', '00000000-0000-0000-0001-000000000176', NULL, NULL);

-- 34: Bruschetta
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000034', :user_id, 'Bruschetta',
     ARRAY['Dice tomatoes and mix with basil, garlic, olive oil', 'Toast bread slices', 'Spoon mixture on top'],
     ARRAY['Italian', 'Quick', 'Vegetarian'], 4);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000034', '00000000-0000-0000-0001-000000000022', NULL, '4 medium'),
    ('00000000-0000-0000-2000-000000000034', '00000000-0000-0000-0001-000000000160', NULL, NULL),
    ('00000000-0000-0000-2000-000000000034', '00000000-0000-0000-0001-000000000025', NULL, '2 cloves'),
    ('00000000-0000-0000-2000-000000000034', '00000000-0000-0000-0001-000000000173', NULL, NULL),
    ('00000000-0000-0000-2000-000000000034', '00000000-0000-0000-0001-000000000085', NULL, '8 slices');

-- 35: Chicken Soup
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000035', :user_id, 'Chicken Soup',
     ARRAY['Simmer chicken in water with herbs', 'Remove chicken and shred', 'Add carrots, celery, onion to broth', 'Return chicken and cook until vegetables tender'],
     ARRAY['Soup', 'Comfort Food', 'Dinner'], 6);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000035', '00000000-0000-0000-0001-000000000061', NULL, '500g'),
    ('00000000-0000-0000-2000-000000000035', '00000000-0000-0000-0001-000000000024', NULL, '2 medium'),
    ('00000000-0000-0000-2000-000000000035', '00000000-0000-0000-0001-000000000034', NULL, '2 stalks'),
    ('00000000-0000-0000-2000-000000000035', '00000000-0000-0000-0001-000000000021', NULL, '1 large'),
    ('00000000-0000-0000-2000-000000000035', '00000000-0000-0000-0001-000000000161', NULL, NULL);

-- 36: Eggplant Parmesan
INSERT INTO recipes (id, user_id, name, description, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000036', :user_id, 'Eggplant Parmesan',
     'https://example.com/eggplant-parm',
     ARRAY['Slice and salt eggplant', 'Bread with flour, egg, breadcrumbs', 'Fry until golden', 'Layer with sauce and cheese, bake'],
     ARRAY['Italian', 'Vegetarian', 'Dinner'], 4);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000036', '00000000-0000-0000-0001-000000000042', NULL, '2 large'),
    ('00000000-0000-0000-2000-000000000036', '00000000-0000-0000-0001-000000000133', NULL, '1 can'),
    ('00000000-0000-0000-2000-000000000036', '00000000-0000-0000-0001-000000000055', NULL, NULL),
    ('00000000-0000-0000-2000-000000000036', '00000000-0000-0000-0001-000000000291', NULL, NULL),
    ('00000000-0000-0000-2000-000000000036', '00000000-0000-0000-0001-000000000049', NULL, '2'),
    ('00000000-0000-0000-2000-000000000036', NULL, 'Breadcrumbs', NULL);

-- 37: Guacamole
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000037', :user_id, 'Guacamole',
     ARRAY['Mash avocados', 'Mix in lime juice, salt, onion, tomato', 'Add cilantro'],
     ARRAY['Mexican', 'Quick', 'Vegetarian'], 4);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000037', '00000000-0000-0000-0001-000000000013', NULL, '3 ripe'),
    ('00000000-0000-0000-2000-000000000037', '00000000-0000-0000-0001-000000000005', NULL, '2'),
    ('00000000-0000-0000-2000-000000000037', '00000000-0000-0000-0001-000000000021', NULL, '1 small'),
    ('00000000-0000-0000-2000-000000000037', '00000000-0000-0000-0001-000000000022', NULL, '1 medium');

-- 38: Potato Leek Soup
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000038', :user_id, 'Potato Leek Soup',
     ARRAY['Sauté leeks in butter', 'Add potatoes and broth', 'Simmer until tender', 'Blend and stir in cream'],
     ARRAY['Soup', 'Comfort Food', 'Vegetarian'], 6);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000038', '00000000-0000-0000-0001-000000000023', NULL, '600g'),
    ('00000000-0000-0000-2000-000000000038', '00000000-0000-0000-0001-000000000043', NULL, '3 large'),
    ('00000000-0000-0000-2000-000000000038', '00000000-0000-0000-0001-000000000047', NULL, NULL),
    ('00000000-0000-0000-2000-000000000038', '00000000-0000-0000-0001-000000000051', NULL, NULL),
    ('00000000-0000-0000-2000-000000000038', NULL, 'Chicken broth', '750ml');

-- 39: Teriyaki Salmon
INSERT INTO recipes (id, user_id, name, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000039', :user_id, 'Teriyaki Salmon',
     ARRAY['Mix soy sauce, honey, ginger for glaze', 'Pan sear salmon', 'Brush with glaze', 'Serve with rice'],
     ARRAY['Asian', 'Dinner', 'Healthy'], 2);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000039', '00000000-0000-0000-0001-000000000075', NULL, '400g'),
    ('00000000-0000-0000-2000-000000000039', '00000000-0000-0000-0001-000000000146', NULL, NULL),
    ('00000000-0000-0000-2000-000000000039', '00000000-0000-0000-0001-000000000223', NULL, NULL),
    ('00000000-0000-0000-2000-000000000039', '00000000-0000-0000-0001-000000000026', NULL, NULL),
    ('00000000-0000-0000-2000-000000000039', '00000000-0000-0000-0001-000000000095', NULL, '1 1/2 cups');

-- 40: Chocolate Mug Cake
INSERT INTO recipes (id, user_id, name, description, steps, tags, servings) VALUES
    ('00000000-0000-0000-2000-000000000040', :user_id, 'Chocolate Mug Cake',
     'Ready in 5 minutes! Perfect for late night cravings.',
     ARRAY['Mix flour, sugar, cocoa powder in a mug', 'Add milk, oil, and egg', 'Stir well', 'Microwave 90 seconds'],
     ARRAY['Sweet', 'Quick', 'Baking'], 1);
INSERT INTO recipe_ingredients (recipe_id, catalog_item_id, free_text_name, quantity) VALUES
    ('00000000-0000-0000-2000-000000000040', '00000000-0000-0000-0001-000000000291', NULL, '3 tbsp'),
    ('00000000-0000-0000-2000-000000000040', '00000000-0000-0000-0001-000000000182', NULL, '2 tbsp'),
    ('00000000-0000-0000-2000-000000000040', NULL, 'Cocoa powder', '1 tbsp'),
    ('00000000-0000-0000-2000-000000000040', '00000000-0000-0000-0001-000000000046', NULL, '3 tbsp'),
    ('00000000-0000-0000-2000-000000000040', '00000000-0000-0000-0001-000000000049', NULL, '1');

-- ============================================================
-- Meal Plan (previous week, current week, next week)
-- ============================================================

-- Helper: Monday of current week = CURRENT_DATE - (EXTRACT(ISODOW FROM CURRENT_DATE)::int - 1)
-- Previous week Monday = that - 7
-- Next week Monday = that + 7

-- Previous week
INSERT INTO meal_plan_items (user_id, day, recipe_id, note) VALUES
    (:user_id, CURRENT_DATE - (EXTRACT(ISODOW FROM CURRENT_DATE)::int - 1) * INTERVAL '1 day' - INTERVAL '7 days',
        '00000000-0000-0000-2000-000000000006', NULL),
    (:user_id, CURRENT_DATE - (EXTRACT(ISODOW FROM CURRENT_DATE)::int - 2) * INTERVAL '1 day' - INTERVAL '7 days',
        '00000000-0000-0000-2000-000000000016', NULL),
    (:user_id, CURRENT_DATE - (EXTRACT(ISODOW FROM CURRENT_DATE)::int - 3) * INTERVAL '1 day' - INTERVAL '7 days',
        '00000000-0000-0000-2000-000000000001', NULL),
    (:user_id, CURRENT_DATE - (EXTRACT(ISODOW FROM CURRENT_DATE)::int - 5) * INTERVAL '1 day' - INTERVAL '7 days',
        NULL, 'Order pizza');

-- Current week
INSERT INTO meal_plan_items (user_id, day, recipe_id, note) VALUES
    (:user_id, CURRENT_DATE - (EXTRACT(ISODOW FROM CURRENT_DATE)::int - 1) * INTERVAL '1 day',
        '00000000-0000-0000-2000-000000000004', NULL),
    (:user_id, CURRENT_DATE - (EXTRACT(ISODOW FROM CURRENT_DATE)::int - 1) * INTERVAL '1 day',
        '00000000-0000-0000-2000-000000000032', NULL),
    (:user_id, CURRENT_DATE - (EXTRACT(ISODOW FROM CURRENT_DATE)::int - 2) * INTERVAL '1 day',
        '00000000-0000-0000-2000-000000000003', NULL),
    (:user_id, CURRENT_DATE - (EXTRACT(ISODOW FROM CURRENT_DATE)::int - 3) * INTERVAL '1 day',
        '00000000-0000-0000-2000-000000000010', NULL),
    (:user_id, CURRENT_DATE - (EXTRACT(ISODOW FROM CURRENT_DATE)::int - 4) * INTERVAL '1 day',
        '00000000-0000-0000-2000-000000000039', NULL),
    (:user_id, CURRENT_DATE - (EXTRACT(ISODOW FROM CURRENT_DATE)::int - 5) * INTERVAL '1 day',
        NULL, 'Dinner at Mario''s'),
    (:user_id, CURRENT_DATE - (EXTRACT(ISODOW FROM CURRENT_DATE)::int - 6) * INTERVAL '1 day',
        NULL, 'https://example.com/saturday-recipe'),
    (:user_id, CURRENT_DATE - (EXTRACT(ISODOW FROM CURRENT_DATE)::int - 7) * INTERVAL '1 day',
        '00000000-0000-0000-2000-000000000006', NULL);

-- Next week
INSERT INTO meal_plan_items (user_id, day, recipe_id, note) VALUES
    (:user_id, CURRENT_DATE - (EXTRACT(ISODOW FROM CURRENT_DATE)::int - 1) * INTERVAL '1 day' + INTERVAL '7 days',
        '00000000-0000-0000-2000-000000000028', NULL),
    (:user_id, CURRENT_DATE - (EXTRACT(ISODOW FROM CURRENT_DATE)::int - 2) * INTERVAL '1 day' + INTERVAL '7 days',
        '00000000-0000-0000-2000-000000000014', NULL),
    (:user_id, CURRENT_DATE - (EXTRACT(ISODOW FROM CURRENT_DATE)::int - 3) * INTERVAL '1 day' + INTERVAL '7 days',
        '00000000-0000-0000-2000-000000000018', NULL),
    (:user_id, CURRENT_DATE - (EXTRACT(ISODOW FROM CURRENT_DATE)::int - 5) * INTERVAL '1 day' + INTERVAL '7 days',
        '00000000-0000-0000-2000-000000000008', NULL);
