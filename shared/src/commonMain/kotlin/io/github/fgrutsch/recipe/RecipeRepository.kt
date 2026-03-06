package io.github.fgrutsch.recipe

import io.github.fgrutsch.catalog.DefaultCategoryIds
import io.github.fgrutsch.catalog.Item
import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
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

private fun ri(item: Item, quantity: Float? = null) = RecipeIngredient(item, quantity)

private fun defaultRecipes(): List<Recipe> = listOf(
    Recipe(
        id = "recipe-pasta",
        name = "Spaghetti Bolognese",
        ingredients = listOf(
            ri(Item.CategorizedItem(id = "item-ground-beef", name = "Ground Beef", category = DefaultCategoryIds.MEAT), 500f),
            ri(Item.CategorizedItem(id = "item-tomatoes", name = "Tomatoes", category = DefaultCategoryIds.VEGETABLES), 400f),
            ri(Item.CategorizedItem(id = "item-onions", name = "Onions", category = DefaultCategoryIds.VEGETABLES), 2f),
            ri(Item.CategorizedItem(id = "item-olive-oil", name = "Olive Oil", category = DefaultCategoryIds.OTHER)),
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
            ri(Item.CategorizedItem(id = "item-cheese", name = "Cheese", category = DefaultCategoryIds.DAIRY), 50f),
            ri(Item.CategorizedItem(id = "item-bread", name = "Bread", category = DefaultCategoryIds.BAKERY), 2f),
            ri(Item.CategorizedItem(id = "item-olive-oil", name = "Olive Oil", category = DefaultCategoryIds.OTHER)),
            ri(Item.CategorizedItem(id = "item-lemons", name = "Lemons", category = DefaultCategoryIds.FRUITS), 1f),
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
            ri(Item.CategorizedItem(id = "item-eggs", name = "Eggs", category = DefaultCategoryIds.DAIRY), 3f),
            ri(Item.CategorizedItem(id = "item-cheese", name = "Cheese", category = DefaultCategoryIds.DAIRY), 50f),
            ri(Item.CategorizedItem(id = "item-butter", name = "Butter", category = DefaultCategoryIds.DAIRY), 1f),
            ri(Item.CategorizedItem(id = "item-salt", name = "Salt", category = DefaultCategoryIds.OTHER)),
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
            ri(Item.CategorizedItem(id = "item-eggs", name = "Eggs", category = DefaultCategoryIds.DAIRY), 4f),
            ri(Item.CategorizedItem(id = "item-cheese", name = "Cheese", category = DefaultCategoryIds.DAIRY), 100f),
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
            ri(Item.CategorizedItem(id = "item-flour", name = "Flour", category = DefaultCategoryIds.BAKERY), 200f),
            ri(Item.CategorizedItem(id = "item-eggs", name = "Eggs", category = DefaultCategoryIds.DAIRY), 2f),
            ri(Item.CategorizedItem(id = "item-milk", name = "Milk", category = DefaultCategoryIds.DAIRY), 250f),
            ri(Item.CategorizedItem(id = "item-butter", name = "Butter", category = DefaultCategoryIds.DAIRY), 30f),
            ri(Item.CategorizedItem(id = "item-sugar", name = "Sugar", category = DefaultCategoryIds.OTHER), 2f),
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
            ri(Item.CategorizedItem(id = "item-chicken", name = "Chicken", category = DefaultCategoryIds.MEAT), 400f),
            ri(Item.CategorizedItem(id = "item-carrots", name = "Carrots", category = DefaultCategoryIds.VEGETABLES), 2f),
            ri(Item.CategorizedItem(id = "item-onions", name = "Onions", category = DefaultCategoryIds.VEGETABLES), 1f),
            ri(Item.CategorizedItem(id = "item-olive-oil", name = "Olive Oil", category = DefaultCategoryIds.OTHER)),
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
            ri(Item.CategorizedItem(id = "item-tomatoes", name = "Tomatoes", category = DefaultCategoryIds.VEGETABLES), 3f),
            ri(Item.CategorizedItem(id = "item-onions", name = "Onions", category = DefaultCategoryIds.VEGETABLES), 1f),
            ri(Item.CategorizedItem(id = "item-cheese", name = "Cheese", category = DefaultCategoryIds.DAIRY), 200f),
            ri(Item.CategorizedItem(id = "item-olive-oil", name = "Olive Oil", category = DefaultCategoryIds.OTHER)),
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
            ri(Item.CategorizedItem(id = "item-chicken", name = "Chicken", category = DefaultCategoryIds.MEAT), 300f),
            ri(Item.CategorizedItem(id = "item-eggs", name = "Eggs", category = DefaultCategoryIds.DAIRY), 2f),
            ri(Item.FreeTextItem(name = "Bean sprouts"), 100f),
            ri(Item.FreeTextItem(name = "Peanuts"), 50f),
            ri(Item.FreeTextItem(name = "Fish sauce"), 3f),
            ri(Item.CategorizedItem(id = "item-lemons", name = "Lemons", category = DefaultCategoryIds.FRUITS), 1f),
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
            ri(Item.CategorizedItem(id = "item-bananas", name = "Bananas", category = DefaultCategoryIds.FRUITS), 2f),
            ri(Item.CategorizedItem(id = "item-milk", name = "Milk", category = DefaultCategoryIds.DAIRY), 250f),
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
            ri(Item.CategorizedItem(id = "item-tomatoes", name = "Tomatoes", category = DefaultCategoryIds.VEGETABLES), 2f),
            ri(Item.CategorizedItem(id = "item-carrots", name = "Carrots", category = DefaultCategoryIds.VEGETABLES), 1f),
            ri(Item.CategorizedItem(id = "item-cheese", name = "Cheese", category = DefaultCategoryIds.DAIRY), 50f),
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
