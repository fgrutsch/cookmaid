CREATE TABLE item_categories (
    id UUID PRIMARY KEY,
    name_en TEXT NOT NULL,
    name_de TEXT NOT NULL
);

INSERT INTO item_categories (id, name_en, name_de) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Fruits', 'Obst'),
    ('00000000-0000-0000-0000-000000000002', 'Vegetables', 'Gemüse'),
    ('00000000-0000-0000-0000-000000000003', 'Bread', 'Brot'),
    ('00000000-0000-0000-0000-000000000004', 'Dairy', 'Milchprodukte'),
    ('00000000-0000-0000-0000-000000000005', 'Meat', 'Fleisch'),
    ('00000000-0000-0000-0000-000000000006', 'Fish', 'Fisch'),
    ('00000000-0000-0000-0000-000000000007', 'Grains & Pasta', 'Getreide & Nudeln'),
    ('00000000-0000-0000-0000-000000000008', 'Beverages', 'Getränke'),
    ('00000000-0000-0000-0000-000000000009', 'Snacks', 'Snacks'),
    ('00000000-0000-0000-0000-000000000010', 'Frozen', 'Tiefkühl'),
    ('00000000-0000-0000-0000-000000000011', 'Canned & Jarred', 'Konserven & Gläser'),
    ('00000000-0000-0000-0000-000000000012', 'Condiments & Sauces', 'Gewürze & Soßen'),
    ('00000000-0000-0000-0000-000000000013', 'Spices & Herbs', 'Gewürze & Kräuter'),
    ('00000000-0000-0000-0000-000000000014', 'Oils & Vinegars', 'Öle & Essig'),
    ('00000000-0000-0000-0000-000000000015', 'Baking', 'Backen'),
    ('00000000-0000-0000-0000-000000000016', 'Household', 'Haushalt');
