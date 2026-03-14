# Cookmaid

An app for managing shopping lists, recipes and meal planning.

## POC

- start with the UI only, using in-memory data structures and repositories
- focus on the shopping list feature first, then add recipes and meal planning later
- ensure to design that everything is user/account specific

## Tech
- follow best practices for clean architecture and separation of concerns, but avoid over-engineering for the POC stage
- ensure to use async data access patterns (suspend), as later this will be backed by a database and remote API
- avoid LaunchedEffects in UI code, the view models should expose all data as state flows and handle all side effects internally, so the UI can just collect and display state. for non data fetching related use cases it is ok
- split code by feature area (shopping, recipes, meal planning) rather than by technical layer (ui, viewmodel, repository) to keep related code together and make it easier to iterate on features (in all gradle projects)
- avoid big classes/files. try to re-use components and break down complex screens into smaller composables and dialogs
- inject dependencies via constructors. use latest koin version. each "entity" (e.g. shopping list, recipe, meal plan) has its own koin module
- import all classes, objects, functions, etc., never use FQNs in the code

## Shopping Lists

- Create, rename and delete shopping lists
- One shopping list is marked as default (used by meal plan "add to shopping list" feature)
- Add items to shopping lists
  - Categorized items from a global catalog (use autosuggest when adding)
  - Free-text items (no category)
- Edit item quantity and checked status
- Delete items from shopping lists (swipe to delete)
- Load item categories and catalog from a global repository (not user specific, seeded with defaults, no user management yet)
- deleting checked items from the shopping list (use a dedicated button/option next to the "Checked" header)

### API

- `GET /api/shopping-lists`
- `GET /api/shopping-lists/{id}/items`
- `POST /api/shopping-lists` (body: name, default)
- `PUT /api/shopping-lists/{id}` (body: name, default)
- `DELETE /api/shopping-lists/{id}`
- `POST /api/shopping-lists/{id}/items` (body: item id or free text, quantity)
- `PUT /api/shopping-lists/{listId}/items/{itemId}` (body: quantity, checked)
- `DELETE /api/shopping-lists/{listId}/items/{itemId}`
- `DELETE /api/shopping-lists/{listId}/items?checked=true` (delete all checked items)

Request models:
- use request models separate from the UI models (if needed)
- share them between the server and client code (e.g. .shopping.api vs shopping.model packages)

Flow:
- when navigating first time to the shopping list tab then load all lists (select the default one)
- Load items for the selected list
- ensure to use a spinner loading state (don't use init {} for loading, make it compose firendly and allow to reload on demand, e.g. via pull to refresh)


Take the below model as a reference.

```kotlin

// Globally managed and stored in a repository (not user specific)
data class ItemCategory(
    val id: Uuid,
    val name: String
)

sealed interface Item {
    val name: String

    // Globally managed and stored in a repository (not user specific)
    data class CatalogItem(
        val id: Uuid,
        override val name: String,
        val category: ItemCategory
    ) : Item


    data class FreeTextItem(
        override val name: String
    ) : Item

}

// For shopping list entries, user-managed and stored in the shopping list repository
data class ShoppingItem(
    val id: Uuid,
    val item: Item,
    val quantity: Float?,
    val checked: Boolean
)

// User-managed shopping list, stored in the shopping list repository
data class ShoppingList(
    val id: Uuid,
    val name: String,
    val default: Boolean
)
```

## Recipes

- Create, rename and delete recipes
- Recipes can have one or multiple tags/cateogries (e.g. Noodles, Meat, etc.) assigned
- Tags/cateogries can be created on the fly when creating/editing repcies, but are user scoped
- Recipe fields (see model below)
- Recipe screen has some option for displaying a random recipe (also some re-roll option if I am not happy with it + an optino to go into the details of the recipe)
- Recipes can be search via a search bar (search by name or tags/categories)
- Recipes are displayed by category in the recipe list screen (uncategorized recipes at the end)
- Recipe ingredients have auto suggest of the shopping list item catalog, but can also be free text (re-use the model/logic if possible)
- there can be a lot of recipies, use some lazy loading technique (UI + API later)
- recipe list: each recipe should have a kebap icon to put the recipe in the shopping list and add it to the meal plan
- recipe detail: in kebap icon, add same options as in recipe list

```kotlin

data class Recipe(
    val id: Uuid,
    val name: String,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
    val tags: List<String>
)

data class RecipeIngredient(
    val item: Item,
    val quantity: Float?
)

```

## Meal Planning

- Allow to add items to a meal plan for each day of the week
- Two types of meal plan items:
  - **RecipeItem**: references a managed recipe (tapping opens recipe detail)
  - **NoteItem**: free text or URL (tapping a URL opens in browser, tapping text opens edit dialog)
- No need for splitting by meal (breakfast/lunch/dinner) for now, just a list of items per day
- Items can be removed via swipe to delete
- Recipe items have kebab menu with "Add to shopping list" option (opens ingredient picker dialog, adds to default shopping list)
- Week-based navigation with arrows and "Today" button in top bar
- Ensure lazy loading for API later
- Follow same design as in other tabs
- UI: day name + date on one line per day card

```kotlin

sealed interface MealPlanItem {
    val id: Uuid

    data class RecipeItem(
        override val id: Uuid,
        val recipeId: Uuid
    ) : MealPlanItem

    data class NoteItem(
        override val id: Uuid,
        val name: String
    ) : MealPlanItem
}
```

## Settings

- allow to logout and return to login screen
- Later: auto dark mode, and store in user settings (api or device storage?)

## Auth
- login screen, with OIDC authentication.
  - use: https://github.com/kalinjul/kotlin-multiplatform-oidc
  - create some user entity upon first login and store user specific data (e.g. shopping lists, recipes, meal plans) linked to that user

## UI

- ensure non null fields are validated on UI
- Later: i18n, fetch strings via api. allow to change via settings
- loading via API can take time, ensure to show loading states (e.g. skeletons) and handle errors gracefully (e.g. show error message and retry option)

## Server

- use ktor server
- use ktor for DI
- everything is "entity based" (similar to UI)
- each entity has its own koin module
