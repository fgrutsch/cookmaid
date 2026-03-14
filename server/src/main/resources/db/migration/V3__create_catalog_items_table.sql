CREATE TABLE catalog_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    category_id UUID NOT NULL REFERENCES item_categories(id)
);

CREATE INDEX idx_catalog_items_category_id ON catalog_items(category_id);
CREATE INDEX idx_catalog_items_name ON catalog_items(name);

-- Fruits
INSERT INTO catalog_items (name, category_id) VALUES
    ('Apples', '1be05e1b-42d5-42a4-be1b-e7ae0fc1cfc5'),
    ('Bananas', '1be05e1b-42d5-42a4-be1b-e7ae0fc1cfc5'),
    ('Oranges', '1be05e1b-42d5-42a4-be1b-e7ae0fc1cfc5'),
    ('Lemons', '1be05e1b-42d5-42a4-be1b-e7ae0fc1cfc5'),
    ('Limes', '1be05e1b-42d5-42a4-be1b-e7ae0fc1cfc5'),
    ('Grapes', '1be05e1b-42d5-42a4-be1b-e7ae0fc1cfc5'),
    ('Strawberries', '1be05e1b-42d5-42a4-be1b-e7ae0fc1cfc5'),
    ('Blueberries', '1be05e1b-42d5-42a4-be1b-e7ae0fc1cfc5'),
    ('Raspberries', '1be05e1b-42d5-42a4-be1b-e7ae0fc1cfc5'),
    ('Watermelon', '1be05e1b-42d5-42a4-be1b-e7ae0fc1cfc5'),
    ('Pineapple', '1be05e1b-42d5-42a4-be1b-e7ae0fc1cfc5'),
    ('Mango', '1be05e1b-42d5-42a4-be1b-e7ae0fc1cfc5'),
    ('Avocado', '1be05e1b-42d5-42a4-be1b-e7ae0fc1cfc5'),
    ('Peaches', '1be05e1b-42d5-42a4-be1b-e7ae0fc1cfc5'),
    ('Pears', '1be05e1b-42d5-42a4-be1b-e7ae0fc1cfc5'),
    ('Cherries', '1be05e1b-42d5-42a4-be1b-e7ae0fc1cfc5'),
    ('Kiwi', '1be05e1b-42d5-42a4-be1b-e7ae0fc1cfc5'),
    ('Plums', '1be05e1b-42d5-42a4-be1b-e7ae0fc1cfc5'),
    ('Grapefruit', '1be05e1b-42d5-42a4-be1b-e7ae0fc1cfc5'),
    ('Coconut', '1be05e1b-42d5-42a4-be1b-e7ae0fc1cfc5');

-- Vegetables
INSERT INTO catalog_items (name, category_id) VALUES
    ('Onions', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Tomatoes', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Potatoes', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Carrots', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Garlic', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Ginger', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Bell Peppers', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Broccoli', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Spinach', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Lettuce', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Cucumber', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Zucchini', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Mushrooms', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Celery', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Corn', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Peas', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Green Beans', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Cabbage', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Cauliflower', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Sweet Potatoes', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Asparagus', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Eggplant', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Leeks', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Radishes', 'd89af949-5dfa-4309-880b-ff3205e99525'),
    ('Beets', 'd89af949-5dfa-4309-880b-ff3205e99525');

-- Dairy
INSERT INTO catalog_items (name, category_id) VALUES
    ('Milk', '6a2326df-0713-4ed3-b348-515c32d671d1'),
    ('Butter', '6a2326df-0713-4ed3-b348-515c32d671d1'),
    ('Cheese', '6a2326df-0713-4ed3-b348-515c32d671d1'),
    ('Eggs', '6a2326df-0713-4ed3-b348-515c32d671d1'),
    ('Yogurt', '6a2326df-0713-4ed3-b348-515c32d671d1'),
    ('Cream', '6a2326df-0713-4ed3-b348-515c32d671d1'),
    ('Sour Cream', '6a2326df-0713-4ed3-b348-515c32d671d1'),
    ('Cream Cheese', '6a2326df-0713-4ed3-b348-515c32d671d1'),
    ('Mozzarella', '6a2326df-0713-4ed3-b348-515c32d671d1'),
    ('Parmesan', '6a2326df-0713-4ed3-b348-515c32d671d1'),
    ('Cheddar', '6a2326df-0713-4ed3-b348-515c32d671d1'),
    ('Feta', '6a2326df-0713-4ed3-b348-515c32d671d1'),
    ('Ricotta', '6a2326df-0713-4ed3-b348-515c32d671d1'),
    ('Whipped Cream', '6a2326df-0713-4ed3-b348-515c32d671d1'),
    ('Cottage Cheese', '6a2326df-0713-4ed3-b348-515c32d671d1');

-- Meat
INSERT INTO catalog_items (name, category_id) VALUES
    ('Chicken Breast', '398cf01b-521d-479a-8fc9-c7d35f5fbab4'),
    ('Chicken Thighs', '398cf01b-521d-479a-8fc9-c7d35f5fbab4'),
    ('Ground Beef', '398cf01b-521d-479a-8fc9-c7d35f5fbab4'),
    ('Beef Steak', '398cf01b-521d-479a-8fc9-c7d35f5fbab4'),
    ('Pork Chops', '398cf01b-521d-479a-8fc9-c7d35f5fbab4'),
    ('Ground Pork', '398cf01b-521d-479a-8fc9-c7d35f5fbab4'),
    ('Bacon', '398cf01b-521d-479a-8fc9-c7d35f5fbab4'),
    ('Sausages', '398cf01b-521d-479a-8fc9-c7d35f5fbab4'),
    ('Ham', '398cf01b-521d-479a-8fc9-c7d35f5fbab4'),
    ('Turkey', '398cf01b-521d-479a-8fc9-c7d35f5fbab4'),
    ('Lamb', '398cf01b-521d-479a-8fc9-c7d35f5fbab4'),
    ('Pancetta', '398cf01b-521d-479a-8fc9-c7d35f5fbab4'),
    ('Salami', '398cf01b-521d-479a-8fc9-c7d35f5fbab4'),
    ('Ground Turkey', '398cf01b-521d-479a-8fc9-c7d35f5fbab4');

-- Seafood
INSERT INTO catalog_items (name, category_id) VALUES
    ('Salmon', '7019196d-245f-4bbc-90d9-7f0e72200787'),
    ('Shrimp', '7019196d-245f-4bbc-90d9-7f0e72200787'),
    ('Tuna', '7019196d-245f-4bbc-90d9-7f0e72200787'),
    ('Cod', '7019196d-245f-4bbc-90d9-7f0e72200787'),
    ('Tilapia', '7019196d-245f-4bbc-90d9-7f0e72200787'),
    ('Crab', '7019196d-245f-4bbc-90d9-7f0e72200787'),
    ('Mussels', '7019196d-245f-4bbc-90d9-7f0e72200787'),
    ('Sardines', '7019196d-245f-4bbc-90d9-7f0e72200787'),
    ('Anchovies', '7019196d-245f-4bbc-90d9-7f0e72200787'),
    ('Calamari', '7019196d-245f-4bbc-90d9-7f0e72200787');

-- Bakery
INSERT INTO catalog_items (name, category_id) VALUES
    ('Bread', '717ed820-d536-488b-87f3-968642529670'),
    ('Baguette', '717ed820-d536-488b-87f3-968642529670'),
    ('Tortillas', '717ed820-d536-488b-87f3-968642529670'),
    ('Pita Bread', '717ed820-d536-488b-87f3-968642529670'),
    ('Rolls', '717ed820-d536-488b-87f3-968642529670'),
    ('Croissants', '717ed820-d536-488b-87f3-968642529670'),
    ('Bagels', '717ed820-d536-488b-87f3-968642529670'),
    ('Muffins', '717ed820-d536-488b-87f3-968642529670'),
    ('Naan', '717ed820-d536-488b-87f3-968642529670'),
    ('Crackers', '717ed820-d536-488b-87f3-968642529670');

-- Grains & Pasta
INSERT INTO catalog_items (name, category_id) VALUES
    ('Rice', '6e2b6bfa-bd4c-4aba-b2b5-38fe00c3e0b2'),
    ('Spaghetti', '6e2b6bfa-bd4c-4aba-b2b5-38fe00c3e0b2'),
    ('Penne', '6e2b6bfa-bd4c-4aba-b2b5-38fe00c3e0b2'),
    ('Fusilli', '6e2b6bfa-bd4c-4aba-b2b5-38fe00c3e0b2'),
    ('Rice Noodles', '6e2b6bfa-bd4c-4aba-b2b5-38fe00c3e0b2'),
    ('Couscous', '6e2b6bfa-bd4c-4aba-b2b5-38fe00c3e0b2'),
    ('Quinoa', '6e2b6bfa-bd4c-4aba-b2b5-38fe00c3e0b2'),
    ('Oats', '6e2b6bfa-bd4c-4aba-b2b5-38fe00c3e0b2'),
    ('Polenta', '6e2b6bfa-bd4c-4aba-b2b5-38fe00c3e0b2'),
    ('Bulgur', '6e2b6bfa-bd4c-4aba-b2b5-38fe00c3e0b2');

-- Beverages
INSERT INTO catalog_items (name, category_id) VALUES
    ('Water', '9ef061c2-698c-4b08-a833-fc75b25e915c'),
    ('Coffee', '9ef061c2-698c-4b08-a833-fc75b25e915c'),
    ('Tea', '9ef061c2-698c-4b08-a833-fc75b25e915c'),
    ('Orange Juice', '9ef061c2-698c-4b08-a833-fc75b25e915c'),
    ('Apple Juice', '9ef061c2-698c-4b08-a833-fc75b25e915c'),
    ('Milk', '9ef061c2-698c-4b08-a833-fc75b25e915c'),
    ('Sparkling Water', '9ef061c2-698c-4b08-a833-fc75b25e915c'),
    ('Lemonade', '9ef061c2-698c-4b08-a833-fc75b25e915c'),
    ('Beer', '9ef061c2-698c-4b08-a833-fc75b25e915c'),
    ('Wine', '9ef061c2-698c-4b08-a833-fc75b25e915c');

-- Snacks
INSERT INTO catalog_items (name, category_id) VALUES
    ('Chips', '4f5a43b7-b589-4fb8-96d3-27b1ebe04346'),
    ('Popcorn', '4f5a43b7-b589-4fb8-96d3-27b1ebe04346'),
    ('Pretzels', '4f5a43b7-b589-4fb8-96d3-27b1ebe04346'),
    ('Chocolate', '4f5a43b7-b589-4fb8-96d3-27b1ebe04346'),
    ('Cookies', '4f5a43b7-b589-4fb8-96d3-27b1ebe04346'),
    ('Granola Bars', '4f5a43b7-b589-4fb8-96d3-27b1ebe04346'),
    ('Trail Mix', '4f5a43b7-b589-4fb8-96d3-27b1ebe04346'),
    ('Dried Fruit', '4f5a43b7-b589-4fb8-96d3-27b1ebe04346'),
    ('Rice Cakes', '4f5a43b7-b589-4fb8-96d3-27b1ebe04346'),
    ('Gummy Bears', '4f5a43b7-b589-4fb8-96d3-27b1ebe04346');

-- Frozen
INSERT INTO catalog_items (name, category_id) VALUES
    ('Frozen Pizza', 'b96f691b-6296-4775-814b-68832572c4a5'),
    ('Frozen Vegetables', 'b96f691b-6296-4775-814b-68832572c4a5'),
    ('Ice Cream', 'b96f691b-6296-4775-814b-68832572c4a5'),
    ('Frozen Berries', 'b96f691b-6296-4775-814b-68832572c4a5'),
    ('Frozen Fish', 'b96f691b-6296-4775-814b-68832572c4a5'),
    ('Frozen Fries', 'b96f691b-6296-4775-814b-68832572c4a5'),
    ('Frozen Spinach', 'b96f691b-6296-4775-814b-68832572c4a5'),
    ('Frozen Shrimp', 'b96f691b-6296-4775-814b-68832572c4a5');

-- Canned & Jarred
INSERT INTO catalog_items (name, category_id) VALUES
    ('Canned Tomatoes', '6d19864b-2b24-41e4-a645-e0d0349dd92b'),
    ('Tomato Paste', '6d19864b-2b24-41e4-a645-e0d0349dd92b'),
    ('Canned Beans', '6d19864b-2b24-41e4-a645-e0d0349dd92b'),
    ('Canned Chickpeas', '6d19864b-2b24-41e4-a645-e0d0349dd92b'),
    ('Canned Corn', '6d19864b-2b24-41e4-a645-e0d0349dd92b'),
    ('Canned Tuna', '6d19864b-2b24-41e4-a645-e0d0349dd92b'),
    ('Olives', '6d19864b-2b24-41e4-a645-e0d0349dd92b'),
    ('Pickles', '6d19864b-2b24-41e4-a645-e0d0349dd92b'),
    ('Coconut Milk', '6d19864b-2b24-41e4-a645-e0d0349dd92b'),
    ('Jam', '6d19864b-2b24-41e4-a645-e0d0349dd92b');

-- Condiments & Sauces
INSERT INTO catalog_items (name, category_id) VALUES
    ('Ketchup', 'b50efa79-fe96-4597-a33d-88ebe36bfef2'),
    ('Mustard', 'b50efa79-fe96-4597-a33d-88ebe36bfef2'),
    ('Mayonnaise', 'b50efa79-fe96-4597-a33d-88ebe36bfef2'),
    ('Soy Sauce', 'b50efa79-fe96-4597-a33d-88ebe36bfef2'),
    ('Hot Sauce', 'b50efa79-fe96-4597-a33d-88ebe36bfef2'),
    ('Worcestershire Sauce', 'b50efa79-fe96-4597-a33d-88ebe36bfef2'),
    ('BBQ Sauce', 'b50efa79-fe96-4597-a33d-88ebe36bfef2'),
    ('Pesto', 'b50efa79-fe96-4597-a33d-88ebe36bfef2'),
    ('Hummus', 'b50efa79-fe96-4597-a33d-88ebe36bfef2'),
    ('Salsa', 'b50efa79-fe96-4597-a33d-88ebe36bfef2'),
    ('Fish Sauce', 'b50efa79-fe96-4597-a33d-88ebe36bfef2'),
    ('Tahini', 'b50efa79-fe96-4597-a33d-88ebe36bfef2');

-- Spices & Herbs
INSERT INTO catalog_items (name, category_id) VALUES
    ('Salt', 'e8a09000-ce0a-4ddf-ac56-d910e98b54f9'),
    ('Black Pepper', 'e8a09000-ce0a-4ddf-ac56-d910e98b54f9'),
    ('Paprika', 'e8a09000-ce0a-4ddf-ac56-d910e98b54f9'),
    ('Cumin', 'e8a09000-ce0a-4ddf-ac56-d910e98b54f9'),
    ('Oregano', 'e8a09000-ce0a-4ddf-ac56-d910e98b54f9'),
    ('Basil', 'e8a09000-ce0a-4ddf-ac56-d910e98b54f9'),
    ('Thyme', 'e8a09000-ce0a-4ddf-ac56-d910e98b54f9'),
    ('Rosemary', 'e8a09000-ce0a-4ddf-ac56-d910e98b54f9'),
    ('Cinnamon', 'e8a09000-ce0a-4ddf-ac56-d910e98b54f9'),
    ('Turmeric', 'e8a09000-ce0a-4ddf-ac56-d910e98b54f9'),
    ('Chili Flakes', 'e8a09000-ce0a-4ddf-ac56-d910e98b54f9'),
    ('Bay Leaves', 'e8a09000-ce0a-4ddf-ac56-d910e98b54f9'),
    ('Nutmeg', 'e8a09000-ce0a-4ddf-ac56-d910e98b54f9'),
    ('Parsley', 'e8a09000-ce0a-4ddf-ac56-d910e98b54f9'),
    ('Cilantro', 'e8a09000-ce0a-4ddf-ac56-d910e98b54f9'),
    ('Dill', 'e8a09000-ce0a-4ddf-ac56-d910e98b54f9'),
    ('Mint', 'e8a09000-ce0a-4ddf-ac56-d910e98b54f9'),
    ('Vanilla Extract', 'e8a09000-ce0a-4ddf-ac56-d910e98b54f9');

-- Oils & Vinegars
INSERT INTO catalog_items (name, category_id) VALUES
    ('Olive Oil', '78208a0b-d376-4e20-b6bf-3c1559fb30a3'),
    ('Vegetable Oil', '78208a0b-d376-4e20-b6bf-3c1559fb30a3'),
    ('Coconut Oil', '78208a0b-d376-4e20-b6bf-3c1559fb30a3'),
    ('Sesame Oil', '78208a0b-d376-4e20-b6bf-3c1559fb30a3'),
    ('Balsamic Vinegar', '78208a0b-d376-4e20-b6bf-3c1559fb30a3'),
    ('White Wine Vinegar', '78208a0b-d376-4e20-b6bf-3c1559fb30a3'),
    ('Apple Cider Vinegar', '78208a0b-d376-4e20-b6bf-3c1559fb30a3'),
    ('Red Wine Vinegar', '78208a0b-d376-4e20-b6bf-3c1559fb30a3');

-- Baking
INSERT INTO catalog_items (name, category_id) VALUES
    ('Flour', '8f78dc93-4209-4b27-877a-e139b1d817a9'),
    ('Sugar', '8f78dc93-4209-4b27-877a-e139b1d817a9'),
    ('Brown Sugar', '8f78dc93-4209-4b27-877a-e139b1d817a9'),
    ('Baking Powder', '8f78dc93-4209-4b27-877a-e139b1d817a9'),
    ('Baking Soda', '8f78dc93-4209-4b27-877a-e139b1d817a9'),
    ('Yeast', '8f78dc93-4209-4b27-877a-e139b1d817a9'),
    ('Cocoa Powder', '8f78dc93-4209-4b27-877a-e139b1d817a9'),
    ('Cornstarch', '8f78dc93-4209-4b27-877a-e139b1d817a9'),
    ('Powdered Sugar', '8f78dc93-4209-4b27-877a-e139b1d817a9'),
    ('Chocolate Chips', '8f78dc93-4209-4b27-877a-e139b1d817a9');

-- Nuts & Seeds
INSERT INTO catalog_items (name, category_id) VALUES
    ('Almonds', '53c7bb39-47f1-4647-b4d2-f2144bc7f556'),
    ('Walnuts', '53c7bb39-47f1-4647-b4d2-f2144bc7f556'),
    ('Peanuts', '53c7bb39-47f1-4647-b4d2-f2144bc7f556'),
    ('Cashews', '53c7bb39-47f1-4647-b4d2-f2144bc7f556'),
    ('Sunflower Seeds', '53c7bb39-47f1-4647-b4d2-f2144bc7f556'),
    ('Chia Seeds', '53c7bb39-47f1-4647-b4d2-f2144bc7f556'),
    ('Flax Seeds', '53c7bb39-47f1-4647-b4d2-f2144bc7f556'),
    ('Pine Nuts', '53c7bb39-47f1-4647-b4d2-f2144bc7f556'),
    ('Peanut Butter', '53c7bb39-47f1-4647-b4d2-f2144bc7f556'),
    ('Almond Butter', '53c7bb39-47f1-4647-b4d2-f2144bc7f556');

-- Deli
INSERT INTO catalog_items (name, category_id) VALUES
    ('Sliced Turkey', 'a7c48e4e-b4ef-4a4a-bfe6-7f8566e1bdd5'),
    ('Sliced Ham', 'a7c48e4e-b4ef-4a4a-bfe6-7f8566e1bdd5'),
    ('Prosciutto', 'a7c48e4e-b4ef-4a4a-bfe6-7f8566e1bdd5'),
    ('Smoked Salmon', 'a7c48e4e-b4ef-4a4a-bfe6-7f8566e1bdd5'),
    ('Roast Beef', 'a7c48e4e-b4ef-4a4a-bfe6-7f8566e1bdd5'),
    ('Chorizo', 'a7c48e4e-b4ef-4a4a-bfe6-7f8566e1bdd5');

-- Household
INSERT INTO catalog_items (name, category_id) VALUES
    ('Paper Towels', '9596cd61-78ea-44cd-840e-0e868bbc0b7d'),
    ('Toilet Paper', '9596cd61-78ea-44cd-840e-0e868bbc0b7d'),
    ('Dish Soap', '9596cd61-78ea-44cd-840e-0e868bbc0b7d'),
    ('Sponges', '9596cd61-78ea-44cd-840e-0e868bbc0b7d'),
    ('Trash Bags', '9596cd61-78ea-44cd-840e-0e868bbc0b7d'),
    ('Aluminum Foil', '9596cd61-78ea-44cd-840e-0e868bbc0b7d'),
    ('Plastic Wrap', '9596cd61-78ea-44cd-840e-0e868bbc0b7d'),
    ('Laundry Detergent', '9596cd61-78ea-44cd-840e-0e868bbc0b7d'),
    ('All-Purpose Cleaner', '9596cd61-78ea-44cd-840e-0e868bbc0b7d'),
    ('Ziplock Bags', '9596cd61-78ea-44cd-840e-0e868bbc0b7d');

-- Personal Care
INSERT INTO catalog_items (name, category_id) VALUES
    ('Toothpaste', '6c55b047-a9b3-4113-b4b4-51eedeaf9187'),
    ('Shampoo', '6c55b047-a9b3-4113-b4b4-51eedeaf9187'),
    ('Soap', '6c55b047-a9b3-4113-b4b4-51eedeaf9187'),
    ('Deodorant', '6c55b047-a9b3-4113-b4b4-51eedeaf9187'),
    ('Hand Cream', '6c55b047-a9b3-4113-b4b4-51eedeaf9187'),
    ('Tissues', '6c55b047-a9b3-4113-b4b4-51eedeaf9187');

-- Other
INSERT INTO catalog_items (name, category_id) VALUES
    ('Honey', '44f11cfb-36de-4d10-8bba-13c8d062f436'),
    ('Maple Syrup', '44f11cfb-36de-4d10-8bba-13c8d062f436'),
    ('Tofu', '44f11cfb-36de-4d10-8bba-13c8d062f436'),
    ('Protein Powder', '44f11cfb-36de-4d10-8bba-13c8d062f436'),
    ('Baby Food', '44f11cfb-36de-4d10-8bba-13c8d062f436');
