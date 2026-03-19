CREATE TABLE meal_plan_items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    day         DATE NOT NULL,
    recipe_id   UUID REFERENCES recipes(id) ON DELETE CASCADE,
    note        TEXT,
    CONSTRAINT meal_plan_item_type_check CHECK (
        (recipe_id IS NOT NULL AND note IS NULL)
        OR (recipe_id IS NULL AND note IS NOT NULL)
    )
);

CREATE INDEX idx_meal_plan_items_user_day ON meal_plan_items(user_id, day);
