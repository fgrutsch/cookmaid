package io.github.fgrutsch.recipe

import io.github.fgrutsch.catalog.Item
import io.github.fgrutsch.catalog.ItemCategory
import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

interface RecipeRepository {
    val recipes: StateFlow<List<Recipe>>
    suspend fun getById(id: String): Recipe?
    suspend fun add(recipe: Recipe)
    suspend fun update(recipe: Recipe)
    suspend fun delete(id: String)
}

class InMemoryRecipeRepository : RecipeRepository {
    private val _recipes = MutableStateFlow(defaultRecipes())
    override val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    override suspend fun getById(id: String): Recipe? {
        return _recipes.value.find { it.id == id }
    }

    override suspend fun add(recipe: Recipe) {
        _recipes.update { it + recipe }
    }

    override suspend fun update(recipe: Recipe) {
        _recipes.update { list -> list.map { if (it.id == recipe.id) recipe else it } }
    }

    override suspend fun delete(id: String) {
        _recipes.update { list -> list.filter { it.id != id } }
    }
}

private fun ci(name: String, categoryName: String) = Item.CatalogItem(
    id = Uuid.random(), name = name, category = ItemCategory(id = Uuid.random(), name = categoryName),
)
private fun ri(item: Item, quantity: Float? = null) = RecipeIngredient(item, quantity)

private fun defaultRecipes(): List<Recipe> = listOf(
    Recipe(
        id = "recipe-pasta",
        name = "Spaghetti Bolognese",
        ingredients = listOf(
            ri(ci("Ground Beef", "Meat"), 500f),
            ri(ci("Tomatoes", "Vegetables"), 400f),
            ri(ci("Onions", "Vegetables"), 2f),
            ri(ci("Olive Oil", "Oils & Vinegars")),
            ri(Item.FreeTextItem(name = "Spaghetti"), 500f),
            ri(Item.FreeTextItem(name = "Garlic"), 3f),
        ),
        steps = listOf(
            "Cook spaghetti according to package instructions",
            "Dice onions and garlic, sauté in olive oil",
            "Add ground beef and brown",
            "Add tomatoes and simmer for 20 minutes",
            "Serve sauce over spaghetti",
        ),
        tags = listOf("Noodles", "Meat"),
    ),
    Recipe(
        id = "recipe-salad",
        name = "Caesar Salad",
        ingredients = listOf(
            ri(Item.FreeTextItem(name = "Romaine lettuce"), 1f),
            ri(ci("Cheese", "Dairy"), 50f),
            ri(ci("Bread", "Bakery"), 2f),
            ri(ci("Olive Oil", "Oils & Vinegars")),
            ri(ci("Lemons", "Fruits"), 1f),
        ),
        steps = listOf(
            "Wash and chop romaine lettuce",
            "Make croutons from bread cubes with olive oil",
            "Prepare dressing with lemon juice and olive oil",
            "Toss lettuce with dressing, top with croutons and cheese",
        ),
        tags = listOf("Salad", "Vegetarian"),
    ),
    Recipe(
        id = "recipe-omelette",
        name = "Cheese Omelette",
        ingredients = listOf(
            ri(ci("Eggs", "Dairy"), 3f),
            ri(ci("Cheese", "Dairy"), 50f),
            ri(ci("Butter", "Dairy"), 1f),
            ri(ci("Salt", "Spices & Herbs")),
        ),
        steps = listOf(
            "Beat eggs with a pinch of salt",
            "Melt butter in a pan over medium heat",
            "Pour in eggs and cook until edges set",
            "Add cheese, fold and serve",
        ),
        tags = listOf("Breakfast"),
    ),
    Recipe(
        id = "recipe-carbonara",
        name = "Pasta Carbonara",
        ingredients = listOf(
            ri(Item.FreeTextItem(name = "Spaghetti"), 400f),
            ri(Item.FreeTextItem(name = "Pancetta"), 150f),
            ri(ci("Eggs", "Dairy"), 4f),
            ri(ci("Cheese", "Dairy"), 100f),
            ri(Item.FreeTextItem(name = "Black pepper")),
        ),
        steps = listOf(
            "Cook spaghetti in salted water until al dente",
            "Fry pancetta until crispy",
            "Mix eggs with grated cheese and pepper",
            "Toss hot pasta with pancetta, then stir in egg mixture off heat",
        ),
        tags = listOf("Noodles", "Meat"),
    ),
    Recipe(
        id = "recipe-pancakes",
        name = "Fluffy Pancakes",
        ingredients = listOf(
            ri(ci("Flour", "Baking"), 200f),
            ri(ci("Eggs", "Dairy"), 2f),
            ri(ci("Milk", "Dairy"), 250f),
            ri(ci("Butter", "Dairy"), 30f),
            ri(ci("Sugar", "Baking"), 2f),
        ),
        steps = listOf(
            "Mix flour, sugar and a pinch of salt",
            "Whisk eggs, milk and melted butter together",
            "Combine wet and dry ingredients, don't overmix",
            "Cook on a buttered pan until bubbles form, then flip",
        ),
        tags = listOf("Breakfast"),
    ),
    Recipe(
        id = "recipe-stir-fry",
        name = "Chicken Stir Fry",
        ingredients = listOf(
            ri(ci("Chicken", "Meat"), 400f),
            ri(ci("Carrots", "Vegetables"), 2f),
            ri(ci("Onions", "Vegetables"), 1f),
            ri(ci("Olive Oil", "Oils & Vinegars")),
            ri(Item.FreeTextItem(name = "Soy sauce"), 3f),
            ri(Item.FreeTextItem(name = "Rice"), 300f),
        ),
        steps = listOf(
            "Slice chicken into strips and season",
            "Stir fry chicken in oil until golden",
            "Add sliced vegetables and cook for 3 minutes",
            "Add soy sauce and toss",
            "Serve over steamed rice",
        ),
        tags = listOf("Meat"),
    ),
    Recipe(
        id = "recipe-greek-salad",
        name = "Greek Salad",
        ingredients = listOf(
            ri(ci("Tomatoes", "Vegetables"), 3f),
            ri(ci("Onions", "Vegetables"), 1f),
            ri(ci("Cheese", "Dairy"), 200f),
            ri(ci("Olive Oil", "Oils & Vinegars")),
            ri(Item.FreeTextItem(name = "Cucumber"), 1f),
            ri(Item.FreeTextItem(name = "Olives"), 100f),
        ),
        steps = listOf(
            "Chop tomatoes, cucumber and onions into chunks",
            "Add olives and cubed feta cheese",
            "Drizzle with olive oil and season with oregano",
        ),
        tags = listOf("Salad", "Vegetarian"),
    ),
    Recipe(
        id = "recipe-pad-thai",
        name = "Pad Thai",
        ingredients = listOf(
            ri(Item.FreeTextItem(name = "Rice noodles"), 250f),
            ri(ci("Chicken", "Meat"), 300f),
            ri(ci("Eggs", "Dairy"), 2f),
            ri(Item.FreeTextItem(name = "Bean sprouts"), 100f),
            ri(Item.FreeTextItem(name = "Peanuts"), 50f),
            ri(Item.FreeTextItem(name = "Fish sauce"), 3f),
            ri(ci("Lemons", "Fruits"), 1f),
        ),
        steps = listOf(
            "Soak rice noodles in warm water until soft",
            "Stir fry chicken until cooked through",
            "Push to side, scramble eggs in the same pan",
            "Add noodles and sauce, toss everything together",
            "Top with bean sprouts, crushed peanuts and lime",
        ),
        tags = listOf("Noodles", "Meat"),
    ),
    Recipe(
        id = "recipe-banana-smoothie",
        name = "Banana Smoothie",
        ingredients = listOf(
            ri(ci("Bananas", "Fruits"), 2f),
            ri(ci("Milk", "Dairy"), 250f),
            ri(Item.FreeTextItem(name = "Honey"), 1f),
            ri(Item.FreeTextItem(name = "Ice cubes")),
        ),
        steps = listOf(
            "Peel and break bananas into chunks",
            "Blend with milk, honey and ice until smooth",
        ),
        tags = listOf("Breakfast", "Vegetarian"),
    ),
    Recipe(
        id = "recipe-veggie-wrap",
        name = "Veggie Wrap",
        ingredients = listOf(
            ri(Item.FreeTextItem(name = "Tortilla"), 2f),
            ri(ci("Tomatoes", "Vegetables"), 2f),
            ri(ci("Carrots", "Vegetables"), 1f),
            ri(ci("Cheese", "Dairy"), 50f),
            ri(Item.FreeTextItem(name = "Lettuce"), 1f),
            ri(Item.FreeTextItem(name = "Hummus"), 100f),
        ),
        steps = listOf(
            "Warm tortilla in a dry pan",
            "Spread hummus across the center",
            "Layer lettuce, sliced tomatoes, grated carrots and cheese",
            "Roll up tightly and cut in half",
        ),
        tags = listOf("Vegetarian"),
    ),
)
