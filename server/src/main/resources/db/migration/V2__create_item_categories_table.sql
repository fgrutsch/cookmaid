CREATE TABLE item_categories (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL
);

INSERT INTO item_categories (id, name) VALUES
    ('1be05e1b-42d5-42a4-be1b-e7ae0fc1cfc5', 'Fruits'),
    ('d89af949-5dfa-4309-880b-ff3205e99525', 'Vegetables'),
    ('6a2326df-0713-4ed3-b348-515c32d671d1', 'Dairy'),
    ('398cf01b-521d-479a-8fc9-c7d35f5fbab4', 'Meat'),
    ('7019196d-245f-4bbc-90d9-7f0e72200787', 'Seafood'),
    ('717ed820-d536-488b-87f3-968642529670', 'Bakery'),
    ('6e2b6bfa-bd4c-4aba-b2b5-38fe00c3e0b2', 'Grains & Pasta'),
    ('9ef061c2-698c-4b08-a833-fc75b25e915c', 'Beverages'),
    ('4f5a43b7-b589-4fb8-96d3-27b1ebe04346', 'Snacks'),
    ('b96f691b-6296-4775-814b-68832572c4a5', 'Frozen'),
    ('6d19864b-2b24-41e4-a645-e0d0349dd92b', 'Canned & Jarred'),
    ('b50efa79-fe96-4597-a33d-88ebe36bfef2', 'Condiments & Sauces'),
    ('e8a09000-ce0a-4ddf-ac56-d910e98b54f9', 'Spices & Herbs'),
    ('78208a0b-d376-4e20-b6bf-3c1559fb30a3', 'Oils & Vinegars'),
    ('8f78dc93-4209-4b27-877a-e139b1d817a9', 'Baking'),
    ('53c7bb39-47f1-4647-b4d2-f2144bc7f556', 'Nuts & Seeds'),
    ('a7c48e4e-b4ef-4a4a-bfe6-7f8566e1bdd5', 'Deli'),
    ('9596cd61-78ea-44cd-840e-0e868bbc0b7d', 'Household'),
    ('6c55b047-a9b3-4113-b4b4-51eedeaf9187', 'Personal Care'),
    ('44f11cfb-36de-4d10-8bba-13c8d062f436', 'Other');
