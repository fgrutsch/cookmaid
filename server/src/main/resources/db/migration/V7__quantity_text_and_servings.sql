ALTER TABLE recipe_ingredients ALTER COLUMN quantity TYPE TEXT USING quantity::text;
ALTER TABLE shopping_items ALTER COLUMN quantity TYPE TEXT USING quantity::text;
ALTER TABLE recipes ADD COLUMN servings INTEGER;
