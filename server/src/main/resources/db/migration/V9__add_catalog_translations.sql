-- Rename item_categories.name to name_en and add name_de
ALTER TABLE item_categories RENAME COLUMN name TO name_en;
ALTER TABLE item_categories ADD COLUMN name_de TEXT NOT NULL DEFAULT '';

-- Rename catalog_items.name to name_en and add name_de
ALTER TABLE catalog_items RENAME COLUMN name TO name_en;
ALTER TABLE catalog_items ADD COLUMN name_de TEXT NOT NULL DEFAULT '';

-- Populate German translations for categories
UPDATE item_categories SET name_de = 'Obst' WHERE name_en = 'Fruits';
UPDATE item_categories SET name_de = 'Gemüse' WHERE name_en = 'Vegetables';
UPDATE item_categories SET name_de = 'Milchprodukte' WHERE name_en = 'Dairy';
UPDATE item_categories SET name_de = 'Fleisch' WHERE name_en = 'Meat';
UPDATE item_categories SET name_de = 'Fisch & Meeresfrüchte' WHERE name_en = 'Seafood';
UPDATE item_categories SET name_de = 'Backwaren' WHERE name_en = 'Bakery';
UPDATE item_categories SET name_de = 'Getreide & Nudeln' WHERE name_en = 'Grains & Pasta';
UPDATE item_categories SET name_de = 'Getränke' WHERE name_en = 'Beverages';
UPDATE item_categories SET name_de = 'Snacks' WHERE name_en = 'Snacks';
UPDATE item_categories SET name_de = 'Tiefkühl' WHERE name_en = 'Frozen';
UPDATE item_categories SET name_de = 'Konserven & Gläser' WHERE name_en = 'Canned & Jarred';
UPDATE item_categories SET name_de = 'Gewürze & Soßen' WHERE name_en = 'Condiments & Sauces';
UPDATE item_categories SET name_de = 'Kräuter & Gewürze' WHERE name_en = 'Spices & Herbs';
UPDATE item_categories SET name_de = 'Öle & Essig' WHERE name_en = 'Oils & Vinegars';
UPDATE item_categories SET name_de = 'Backen' WHERE name_en = 'Baking';
UPDATE item_categories SET name_de = 'Nüsse & Samen' WHERE name_en = 'Nuts & Seeds';
UPDATE item_categories SET name_de = 'Feinkost' WHERE name_en = 'Deli';
UPDATE item_categories SET name_de = 'Haushalt' WHERE name_en = 'Household';
UPDATE item_categories SET name_de = 'Körperpflege' WHERE name_en = 'Personal Care';
UPDATE item_categories SET name_de = 'Sonstiges' WHERE name_en = 'Other';

-- Populate German translations for catalog items

-- Fruits
UPDATE catalog_items SET name_de = 'Äpfel' WHERE name_en = 'Apples';
UPDATE catalog_items SET name_de = 'Bananen' WHERE name_en = 'Bananas';
UPDATE catalog_items SET name_de = 'Orangen' WHERE name_en = 'Oranges';
UPDATE catalog_items SET name_de = 'Zitronen' WHERE name_en = 'Lemons';
UPDATE catalog_items SET name_de = 'Limetten' WHERE name_en = 'Limes';
UPDATE catalog_items SET name_de = 'Trauben' WHERE name_en = 'Grapes';
UPDATE catalog_items SET name_de = 'Erdbeeren' WHERE name_en = 'Strawberries';
UPDATE catalog_items SET name_de = 'Heidelbeeren' WHERE name_en = 'Blueberries';
UPDATE catalog_items SET name_de = 'Himbeeren' WHERE name_en = 'Raspberries';
UPDATE catalog_items SET name_de = 'Wassermelone' WHERE name_en = 'Watermelon';
UPDATE catalog_items SET name_de = 'Ananas' WHERE name_en = 'Pineapple';
UPDATE catalog_items SET name_de = 'Mango' WHERE name_en = 'Mango';
UPDATE catalog_items SET name_de = 'Avocado' WHERE name_en = 'Avocado';
UPDATE catalog_items SET name_de = 'Pfirsiche' WHERE name_en = 'Peaches';
UPDATE catalog_items SET name_de = 'Birnen' WHERE name_en = 'Pears';
UPDATE catalog_items SET name_de = 'Kirschen' WHERE name_en = 'Cherries';
UPDATE catalog_items SET name_de = 'Kiwi' WHERE name_en = 'Kiwi';
UPDATE catalog_items SET name_de = 'Pflaumen' WHERE name_en = 'Plums';
UPDATE catalog_items SET name_de = 'Grapefruit' WHERE name_en = 'Grapefruit';
UPDATE catalog_items SET name_de = 'Kokosnuss' WHERE name_en = 'Coconut';

-- Vegetables
UPDATE catalog_items SET name_de = 'Zwiebeln' WHERE name_en = 'Onions';
UPDATE catalog_items SET name_de = 'Tomaten' WHERE name_en = 'Tomatoes';
UPDATE catalog_items SET name_de = 'Kartoffeln' WHERE name_en = 'Potatoes';
UPDATE catalog_items SET name_de = 'Karotten' WHERE name_en = 'Carrots';
UPDATE catalog_items SET name_de = 'Knoblauch' WHERE name_en = 'Garlic';
UPDATE catalog_items SET name_de = 'Ingwer' WHERE name_en = 'Ginger';
UPDATE catalog_items SET name_de = 'Paprika' WHERE name_en = 'Bell Peppers';
UPDATE catalog_items SET name_de = 'Brokkoli' WHERE name_en = 'Broccoli';
UPDATE catalog_items SET name_de = 'Spinat' WHERE name_en = 'Spinach';
UPDATE catalog_items SET name_de = 'Kopfsalat' WHERE name_en = 'Lettuce';
UPDATE catalog_items SET name_de = 'Gurke' WHERE name_en = 'Cucumber';
UPDATE catalog_items SET name_de = 'Zucchini' WHERE name_en = 'Zucchini';
UPDATE catalog_items SET name_de = 'Pilze' WHERE name_en = 'Mushrooms';
UPDATE catalog_items SET name_de = 'Sellerie' WHERE name_en = 'Celery';
UPDATE catalog_items SET name_de = 'Mais' WHERE name_en = 'Corn';
UPDATE catalog_items SET name_de = 'Erbsen' WHERE name_en = 'Peas';
UPDATE catalog_items SET name_de = 'Grüne Bohnen' WHERE name_en = 'Green Beans';
UPDATE catalog_items SET name_de = 'Kohl' WHERE name_en = 'Cabbage';
UPDATE catalog_items SET name_de = 'Blumenkohl' WHERE name_en = 'Cauliflower';
UPDATE catalog_items SET name_de = 'Süßkartoffeln' WHERE name_en = 'Sweet Potatoes';
UPDATE catalog_items SET name_de = 'Spargel' WHERE name_en = 'Asparagus';
UPDATE catalog_items SET name_de = 'Aubergine' WHERE name_en = 'Eggplant';
UPDATE catalog_items SET name_de = 'Lauch' WHERE name_en = 'Leeks';
UPDATE catalog_items SET name_de = 'Radieschen' WHERE name_en = 'Radishes';
UPDATE catalog_items SET name_de = 'Rote Bete' WHERE name_en = 'Beets';

-- Dairy
UPDATE catalog_items SET name_de = 'Milch' WHERE name_en = 'Milk' AND category_id = '6a2326df-0713-4ed3-b348-515c32d671d1';
UPDATE catalog_items SET name_de = 'Butter' WHERE name_en = 'Butter';
UPDATE catalog_items SET name_de = 'Käse' WHERE name_en = 'Cheese';
UPDATE catalog_items SET name_de = 'Eier' WHERE name_en = 'Eggs';
UPDATE catalog_items SET name_de = 'Joghurt' WHERE name_en = 'Yogurt';
UPDATE catalog_items SET name_de = 'Sahne' WHERE name_en = 'Cream';
UPDATE catalog_items SET name_de = 'Sauerrahm' WHERE name_en = 'Sour Cream';
UPDATE catalog_items SET name_de = 'Frischkäse' WHERE name_en = 'Cream Cheese';
UPDATE catalog_items SET name_de = 'Mozzarella' WHERE name_en = 'Mozzarella';
UPDATE catalog_items SET name_de = 'Parmesan' WHERE name_en = 'Parmesan';
UPDATE catalog_items SET name_de = 'Cheddar' WHERE name_en = 'Cheddar';
UPDATE catalog_items SET name_de = 'Feta' WHERE name_en = 'Feta';
UPDATE catalog_items SET name_de = 'Ricotta' WHERE name_en = 'Ricotta';
UPDATE catalog_items SET name_de = 'Schlagsahne' WHERE name_en = 'Whipped Cream';
UPDATE catalog_items SET name_de = 'Hüttenkäse' WHERE name_en = 'Cottage Cheese';

-- Meat
UPDATE catalog_items SET name_de = 'Hühnerbrust' WHERE name_en = 'Chicken Breast';
UPDATE catalog_items SET name_de = 'Hähnchenschenkel' WHERE name_en = 'Chicken Thighs';
UPDATE catalog_items SET name_de = 'Rinderhackfleisch' WHERE name_en = 'Ground Beef';
UPDATE catalog_items SET name_de = 'Rindersteak' WHERE name_en = 'Beef Steak';
UPDATE catalog_items SET name_de = 'Schweinekoteletts' WHERE name_en = 'Pork Chops';
UPDATE catalog_items SET name_de = 'Schweinehackfleisch' WHERE name_en = 'Ground Pork';
UPDATE catalog_items SET name_de = 'Speck' WHERE name_en = 'Bacon';
UPDATE catalog_items SET name_de = 'Würstchen' WHERE name_en = 'Sausages';
UPDATE catalog_items SET name_de = 'Schinken' WHERE name_en = 'Ham';
UPDATE catalog_items SET name_de = 'Truthahn' WHERE name_en = 'Turkey';
UPDATE catalog_items SET name_de = 'Lamm' WHERE name_en = 'Lamb';
UPDATE catalog_items SET name_de = 'Pancetta' WHERE name_en = 'Pancetta';
UPDATE catalog_items SET name_de = 'Salami' WHERE name_en = 'Salami';
UPDATE catalog_items SET name_de = 'Putenhackfleisch' WHERE name_en = 'Ground Turkey';

-- Seafood
UPDATE catalog_items SET name_de = 'Lachs' WHERE name_en = 'Salmon';
UPDATE catalog_items SET name_de = 'Garnelen' WHERE name_en = 'Shrimp';
UPDATE catalog_items SET name_de = 'Thunfisch' WHERE name_en = 'Tuna';
UPDATE catalog_items SET name_de = 'Kabeljau' WHERE name_en = 'Cod';
UPDATE catalog_items SET name_de = 'Tilapia' WHERE name_en = 'Tilapia';
UPDATE catalog_items SET name_de = 'Krabbe' WHERE name_en = 'Crab';
UPDATE catalog_items SET name_de = 'Miesmuscheln' WHERE name_en = 'Mussels';
UPDATE catalog_items SET name_de = 'Sardinen' WHERE name_en = 'Sardines';
UPDATE catalog_items SET name_de = 'Sardellen' WHERE name_en = 'Anchovies';
UPDATE catalog_items SET name_de = 'Calamari' WHERE name_en = 'Calamari';

-- Bakery
UPDATE catalog_items SET name_de = 'Brot' WHERE name_en = 'Bread';
UPDATE catalog_items SET name_de = 'Baguette' WHERE name_en = 'Baguette';
UPDATE catalog_items SET name_de = 'Tortillas' WHERE name_en = 'Tortillas';
UPDATE catalog_items SET name_de = 'Fladenbrot' WHERE name_en = 'Pita Bread';
UPDATE catalog_items SET name_de = 'Brötchen' WHERE name_en = 'Rolls';
UPDATE catalog_items SET name_de = 'Croissants' WHERE name_en = 'Croissants';
UPDATE catalog_items SET name_de = 'Bagels' WHERE name_en = 'Bagels';
UPDATE catalog_items SET name_de = 'Muffins' WHERE name_en = 'Muffins';
UPDATE catalog_items SET name_de = 'Naan' WHERE name_en = 'Naan';
UPDATE catalog_items SET name_de = 'Cracker' WHERE name_en = 'Crackers';

-- Grains & Pasta
UPDATE catalog_items SET name_de = 'Reis' WHERE name_en = 'Rice';
UPDATE catalog_items SET name_de = 'Spaghetti' WHERE name_en = 'Spaghetti';
UPDATE catalog_items SET name_de = 'Penne' WHERE name_en = 'Penne';
UPDATE catalog_items SET name_de = 'Fusilli' WHERE name_en = 'Fusilli';
UPDATE catalog_items SET name_de = 'Reisnudeln' WHERE name_en = 'Rice Noodles';
UPDATE catalog_items SET name_de = 'Couscous' WHERE name_en = 'Couscous';
UPDATE catalog_items SET name_de = 'Quinoa' WHERE name_en = 'Quinoa';
UPDATE catalog_items SET name_de = 'Haferflocken' WHERE name_en = 'Oats';
UPDATE catalog_items SET name_de = 'Polenta' WHERE name_en = 'Polenta';
UPDATE catalog_items SET name_de = 'Bulgur' WHERE name_en = 'Bulgur';

-- Beverages
UPDATE catalog_items SET name_de = 'Wasser' WHERE name_en = 'Water';
UPDATE catalog_items SET name_de = 'Kaffee' WHERE name_en = 'Coffee';
UPDATE catalog_items SET name_de = 'Tee' WHERE name_en = 'Tea';
UPDATE catalog_items SET name_de = 'Orangensaft' WHERE name_en = 'Orange Juice';
UPDATE catalog_items SET name_de = 'Apfelsaft' WHERE name_en = 'Apple Juice';
UPDATE catalog_items SET name_de = 'Milch' WHERE name_en = 'Milk' AND category_id = '9ef061c2-698c-4b08-a833-fc75b25e915c';
UPDATE catalog_items SET name_de = 'Sprudelwasser' WHERE name_en = 'Sparkling Water';
UPDATE catalog_items SET name_de = 'Limonade' WHERE name_en = 'Lemonade';
UPDATE catalog_items SET name_de = 'Bier' WHERE name_en = 'Beer';
UPDATE catalog_items SET name_de = 'Wein' WHERE name_en = 'Wine';

-- Snacks
UPDATE catalog_items SET name_de = 'Chips' WHERE name_en = 'Chips';
UPDATE catalog_items SET name_de = 'Popcorn' WHERE name_en = 'Popcorn';
UPDATE catalog_items SET name_de = 'Brezeln' WHERE name_en = 'Pretzels';
UPDATE catalog_items SET name_de = 'Schokolade' WHERE name_en = 'Chocolate';
UPDATE catalog_items SET name_de = 'Kekse' WHERE name_en = 'Cookies';
UPDATE catalog_items SET name_de = 'Müsliriegel' WHERE name_en = 'Granola Bars';
UPDATE catalog_items SET name_de = 'Studentenfutter' WHERE name_en = 'Trail Mix';
UPDATE catalog_items SET name_de = 'Trockenfrüchte' WHERE name_en = 'Dried Fruit';
UPDATE catalog_items SET name_de = 'Reiswaffeln' WHERE name_en = 'Rice Cakes';
UPDATE catalog_items SET name_de = 'Gummibärchen' WHERE name_en = 'Gummy Bears';

-- Frozen
UPDATE catalog_items SET name_de = 'Tiefkühlpizza' WHERE name_en = 'Frozen Pizza';
UPDATE catalog_items SET name_de = 'Tiefkühlgemüse' WHERE name_en = 'Frozen Vegetables';
UPDATE catalog_items SET name_de = 'Eiscreme' WHERE name_en = 'Ice Cream';
UPDATE catalog_items SET name_de = 'Tiefkühlbeeren' WHERE name_en = 'Frozen Berries';
UPDATE catalog_items SET name_de = 'Tiefkühlfisch' WHERE name_en = 'Frozen Fish';
UPDATE catalog_items SET name_de = 'Tiefkühlpommes' WHERE name_en = 'Frozen Fries';
UPDATE catalog_items SET name_de = 'Tiefkühlspinat' WHERE name_en = 'Frozen Spinach';
UPDATE catalog_items SET name_de = 'Tiefkühlgarnelen' WHERE name_en = 'Frozen Shrimp';

-- Canned & Jarred
UPDATE catalog_items SET name_de = 'Dosentomaten' WHERE name_en = 'Canned Tomatoes';
UPDATE catalog_items SET name_de = 'Tomatenmark' WHERE name_en = 'Tomato Paste';
UPDATE catalog_items SET name_de = 'Dosenbohnen' WHERE name_en = 'Canned Beans';
UPDATE catalog_items SET name_de = 'Dosenkichererbsen' WHERE name_en = 'Canned Chickpeas';
UPDATE catalog_items SET name_de = 'Dosenmais' WHERE name_en = 'Canned Corn';
UPDATE catalog_items SET name_de = 'Dosenthunfisch' WHERE name_en = 'Canned Tuna';
UPDATE catalog_items SET name_de = 'Oliven' WHERE name_en = 'Olives';
UPDATE catalog_items SET name_de = 'Essiggurken' WHERE name_en = 'Pickles';
UPDATE catalog_items SET name_de = 'Kokosmilch' WHERE name_en = 'Coconut Milk';
UPDATE catalog_items SET name_de = 'Marmelade' WHERE name_en = 'Jam';

-- Condiments & Sauces
UPDATE catalog_items SET name_de = 'Ketchup' WHERE name_en = 'Ketchup';
UPDATE catalog_items SET name_de = 'Senf' WHERE name_en = 'Mustard';
UPDATE catalog_items SET name_de = 'Mayonnaise' WHERE name_en = 'Mayonnaise';
UPDATE catalog_items SET name_de = 'Sojasoße' WHERE name_en = 'Soy Sauce';
UPDATE catalog_items SET name_de = 'Scharfe Soße' WHERE name_en = 'Hot Sauce';
UPDATE catalog_items SET name_de = 'Worcestershire-Soße' WHERE name_en = 'Worcestershire Sauce';
UPDATE catalog_items SET name_de = 'BBQ-Soße' WHERE name_en = 'BBQ Sauce';
UPDATE catalog_items SET name_de = 'Pesto' WHERE name_en = 'Pesto';
UPDATE catalog_items SET name_de = 'Hummus' WHERE name_en = 'Hummus';
UPDATE catalog_items SET name_de = 'Salsa' WHERE name_en = 'Salsa';
UPDATE catalog_items SET name_de = 'Fischsoße' WHERE name_en = 'Fish Sauce';
UPDATE catalog_items SET name_de = 'Tahini' WHERE name_en = 'Tahini';

-- Spices & Herbs
UPDATE catalog_items SET name_de = 'Salz' WHERE name_en = 'Salt';
UPDATE catalog_items SET name_de = 'Schwarzer Pfeffer' WHERE name_en = 'Black Pepper';
UPDATE catalog_items SET name_de = 'Paprikapulver' WHERE name_en = 'Paprika';
UPDATE catalog_items SET name_de = 'Kreuzkümmel' WHERE name_en = 'Cumin';
UPDATE catalog_items SET name_de = 'Oregano' WHERE name_en = 'Oregano';
UPDATE catalog_items SET name_de = 'Basilikum' WHERE name_en = 'Basil';
UPDATE catalog_items SET name_de = 'Thymian' WHERE name_en = 'Thyme';
UPDATE catalog_items SET name_de = 'Rosmarin' WHERE name_en = 'Rosemary';
UPDATE catalog_items SET name_de = 'Zimt' WHERE name_en = 'Cinnamon';
UPDATE catalog_items SET name_de = 'Kurkuma' WHERE name_en = 'Turmeric';
UPDATE catalog_items SET name_de = 'Chiliflocken' WHERE name_en = 'Chili Flakes';
UPDATE catalog_items SET name_de = 'Lorbeerblätter' WHERE name_en = 'Bay Leaves';
UPDATE catalog_items SET name_de = 'Muskatnuss' WHERE name_en = 'Nutmeg';
UPDATE catalog_items SET name_de = 'Petersilie' WHERE name_en = 'Parsley';
UPDATE catalog_items SET name_de = 'Koriander' WHERE name_en = 'Cilantro';
UPDATE catalog_items SET name_de = 'Dill' WHERE name_en = 'Dill';
UPDATE catalog_items SET name_de = 'Minze' WHERE name_en = 'Mint';
UPDATE catalog_items SET name_de = 'Vanilleextrakt' WHERE name_en = 'Vanilla Extract';

-- Oils & Vinegars
UPDATE catalog_items SET name_de = 'Olivenöl' WHERE name_en = 'Olive Oil';
UPDATE catalog_items SET name_de = 'Pflanzenöl' WHERE name_en = 'Vegetable Oil';
UPDATE catalog_items SET name_de = 'Kokosöl' WHERE name_en = 'Coconut Oil';
UPDATE catalog_items SET name_de = 'Sesamöl' WHERE name_en = 'Sesame Oil';
UPDATE catalog_items SET name_de = 'Balsamico-Essig' WHERE name_en = 'Balsamic Vinegar';
UPDATE catalog_items SET name_de = 'Weißweinessig' WHERE name_en = 'White Wine Vinegar';
UPDATE catalog_items SET name_de = 'Apfelessig' WHERE name_en = 'Apple Cider Vinegar';
UPDATE catalog_items SET name_de = 'Rotweinessig' WHERE name_en = 'Red Wine Vinegar';

-- Baking
UPDATE catalog_items SET name_de = 'Mehl' WHERE name_en = 'Flour';
UPDATE catalog_items SET name_de = 'Zucker' WHERE name_en = 'Sugar';
UPDATE catalog_items SET name_de = 'Brauner Zucker' WHERE name_en = 'Brown Sugar';
UPDATE catalog_items SET name_de = 'Backpulver' WHERE name_en = 'Baking Powder';
UPDATE catalog_items SET name_de = 'Natron' WHERE name_en = 'Baking Soda';
UPDATE catalog_items SET name_de = 'Hefe' WHERE name_en = 'Yeast';
UPDATE catalog_items SET name_de = 'Kakaopulver' WHERE name_en = 'Cocoa Powder';
UPDATE catalog_items SET name_de = 'Maisstärke' WHERE name_en = 'Cornstarch';
UPDATE catalog_items SET name_de = 'Puderzucker' WHERE name_en = 'Powdered Sugar';
UPDATE catalog_items SET name_de = 'Schokoladenstückchen' WHERE name_en = 'Chocolate Chips';

-- Nuts & Seeds
UPDATE catalog_items SET name_de = 'Mandeln' WHERE name_en = 'Almonds';
UPDATE catalog_items SET name_de = 'Walnüsse' WHERE name_en = 'Walnuts';
UPDATE catalog_items SET name_de = 'Erdnüsse' WHERE name_en = 'Peanuts';
UPDATE catalog_items SET name_de = 'Cashewnüsse' WHERE name_en = 'Cashews';
UPDATE catalog_items SET name_de = 'Sonnenblumenkerne' WHERE name_en = 'Sunflower Seeds';
UPDATE catalog_items SET name_de = 'Chiasamen' WHERE name_en = 'Chia Seeds';
UPDATE catalog_items SET name_de = 'Leinsamen' WHERE name_en = 'Flax Seeds';
UPDATE catalog_items SET name_de = 'Pinienkerne' WHERE name_en = 'Pine Nuts';
UPDATE catalog_items SET name_de = 'Erdnussbutter' WHERE name_en = 'Peanut Butter';
UPDATE catalog_items SET name_de = 'Mandelbutter' WHERE name_en = 'Almond Butter';

-- Deli
UPDATE catalog_items SET name_de = 'Putenbrust' WHERE name_en = 'Sliced Turkey';
UPDATE catalog_items SET name_de = 'Kochschinken' WHERE name_en = 'Sliced Ham';
UPDATE catalog_items SET name_de = 'Prosciutto' WHERE name_en = 'Prosciutto';
UPDATE catalog_items SET name_de = 'Räucherlachs' WHERE name_en = 'Smoked Salmon';
UPDATE catalog_items SET name_de = 'Roastbeef' WHERE name_en = 'Roast Beef';
UPDATE catalog_items SET name_de = 'Chorizo' WHERE name_en = 'Chorizo';

-- Household
UPDATE catalog_items SET name_de = 'Küchenpapier' WHERE name_en = 'Paper Towels';
UPDATE catalog_items SET name_de = 'Toilettenpapier' WHERE name_en = 'Toilet Paper';
UPDATE catalog_items SET name_de = 'Spülmittel' WHERE name_en = 'Dish Soap';
UPDATE catalog_items SET name_de = 'Schwämme' WHERE name_en = 'Sponges';
UPDATE catalog_items SET name_de = 'Müllbeutel' WHERE name_en = 'Trash Bags';
UPDATE catalog_items SET name_de = 'Alufolie' WHERE name_en = 'Aluminum Foil';
UPDATE catalog_items SET name_de = 'Frischhaltefolie' WHERE name_en = 'Plastic Wrap';
UPDATE catalog_items SET name_de = 'Waschmittel' WHERE name_en = 'Laundry Detergent';
UPDATE catalog_items SET name_de = 'Allzweckreiniger' WHERE name_en = 'All-Purpose Cleaner';
UPDATE catalog_items SET name_de = 'Gefrierbeutel' WHERE name_en = 'Ziplock Bags';

-- Personal Care
UPDATE catalog_items SET name_de = 'Zahnpasta' WHERE name_en = 'Toothpaste';
UPDATE catalog_items SET name_de = 'Shampoo' WHERE name_en = 'Shampoo';
UPDATE catalog_items SET name_de = 'Seife' WHERE name_en = 'Soap';
UPDATE catalog_items SET name_de = 'Deodorant' WHERE name_en = 'Deodorant';
UPDATE catalog_items SET name_de = 'Handcreme' WHERE name_en = 'Hand Cream';
UPDATE catalog_items SET name_de = 'Taschentücher' WHERE name_en = 'Tissues';

-- Other
UPDATE catalog_items SET name_de = 'Honig' WHERE name_en = 'Honey';
UPDATE catalog_items SET name_de = 'Ahornsirup' WHERE name_en = 'Maple Syrup';
UPDATE catalog_items SET name_de = 'Tofu' WHERE name_en = 'Tofu';
UPDATE catalog_items SET name_de = 'Proteinpulver' WHERE name_en = 'Protein Powder';
UPDATE catalog_items SET name_de = 'Babynahrung' WHERE name_en = 'Baby Food';
