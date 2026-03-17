CREATE TABLE recipes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    steps TEXT[] NOT NULL DEFAULT '{}',
    tags TEXT[] NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_recipes_user_id ON recipes(user_id);

CREATE TABLE recipe_ingredients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    catalog_item_id UUID REFERENCES catalog_items(id),
    free_text_name TEXT,
    quantity REAL,
    CONSTRAINT recipe_ingredient_type_check CHECK (
        (catalog_item_id IS NOT NULL AND free_text_name IS NULL)
        OR (catalog_item_id IS NULL AND free_text_name IS NOT NULL)
    )
);

CREATE INDEX idx_recipe_ingredients_recipe_id ON recipe_ingredients(recipe_id);
