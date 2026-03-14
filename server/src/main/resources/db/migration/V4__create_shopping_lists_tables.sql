CREATE TABLE shopping_lists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_shopping_lists_user_id ON shopping_lists(user_id);

CREATE TABLE shopping_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    list_id UUID NOT NULL REFERENCES shopping_lists(id) ON DELETE CASCADE,
    catalog_item_id UUID REFERENCES catalog_items(id),
    free_text_name TEXT,
    quantity REAL,
    checked BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT item_type_check CHECK (
        (catalog_item_id IS NOT NULL AND free_text_name IS NULL)
        OR (catalog_item_id IS NULL AND free_text_name IS NOT NULL)
    )
);

CREATE INDEX idx_shopping_items_list_id ON shopping_items(list_id);
