-- Authored order for a recipe's ingredients.
--
-- Every ingredient of a recipe is inserted in one transaction, so created_at is identical across
-- the set and cannot order them; the joins in the read path mean an unordered scan does not
-- return insertion order either. Only an explicit index does.
ALTER TABLE recipe_ingredients
    ADD COLUMN sort_index INTEGER NOT NULL DEFAULT 0;

-- Existing rows have no recoverable authored order. Number them by (created_at, id) so the result
-- is at least stable from now on.
WITH numbered AS (
    SELECT id, row_number() OVER (PARTITION BY recipe_id ORDER BY created_at, id) - 1 AS n
    FROM recipe_ingredients
)
UPDATE recipe_ingredients
SET sort_index = numbered.n
FROM numbered
WHERE recipe_ingredients.id = numbered.id;
