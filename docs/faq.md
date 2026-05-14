# Cookmaid — FAQ & Feature Guide

Cookmaid is a self-hosted meal planning app. This guide covers common
workflows and answers frequently asked questions.

## Shopping List

### How do I add an item?

Type the item name in the input field at the top and tap the send
button (or press Enter). If a matching catalog item exists, it is
used automatically.

### How do I edit an item's quantity?

Swipe the item to the right to open the edit dialog where you can
change the quantity.

### How do I check off items?

Tap an item to mark it as done. Done items move to the bottom of
the list.

### How do I delete items?

Swipe an item to the left to delete it. To remove all checked items
at once, tap the trash icon above all checked items.

### Can I have multiple shopping lists?

Yes. Use the filter chips at the top to switch between lists. Tap
the menu (three dots) to create a new list, rename the current one,
or delete it. The default list cannot be renamed or deleted.

### How do I add recipe ingredients to the shopping list?

From a recipe's detail screen or the recipe list, open the menu
(three dots) and choose "Add to shopping list". On the meal plan,
you can also add a recipe's ingredients via the item menu.

## Recipes

### How do I add a recipe?

Tap the **+** button on the recipe list screen. Fill in the name,
description, ingredients, steps, tags, and servings.

### How do I edit or delete a recipe?

Open the recipe and tap the menu (three dots) in the top bar. Choose
**Edit** to modify or **Delete** to remove the recipe. Swipe-to-delete
is not available on the recipe list.

### Can I search for recipes?

Yes. Tap the search icon in the top bar to search by name. You can
also filter by tags using the filter icon.

### How do I get a random recipe suggestion?

Tap the dice icon on the recipe list screen. A dialog shows a random
recipe with options to view details, reroll, add ingredients to the
shopping list, or add to the meal plan.

## Meal Planning

### How does the meal plan work?

The meal plan shows a weekly view. Each day has a card where you can
add recipes or free-text notes. Navigate between weeks using the
arrow buttons, or jump to the current week with the today icon.

### How do I add items to a day?

Tap the **+** button on a day card. Choose between adding a recipe
(searchable) or a note (free text).

### Can I edit notes?

Yes. Tap a note item to edit its text. Recipe items open the recipe
detail screen instead.

### How do I delete a meal plan item?

Swipe the item to the left to delete it.

### Can I move items between days?

Not directly. Delete the item from one day and add it to the other.

### Can I add ingredients to the shopping list from here?

Yes. On a recipe item, tap the menu (three dots) and choose
"Add to shopping list". You can pick which ingredients to include.

## General

### What platforms does Cookmaid support?

- **Web** — Progressive Web App (works in any modern browser,
  installable on desktop and mobile)
- **Android** — native app via Google Play or APK sideload

### Do I need an account?

Yes. Cookmaid uses OpenID Connect (OIDC) for authentication. Your
admin sets up the identity provider (e.g., PocketID, Keycloak). You
log in with the credentials from that provider.

### Is my data stored locally?

No. All data is stored on the server in a PostgreSQL database. The
app syncs with the server whenever you make changes.

### Can multiple users share the same instance?

Each authenticated user has their own recipes, meal plans, and
shopping lists. Users on the same server do not see each other's data.

### Can I change the theme or language?

Yes. In Settings you can switch between light, dark, and auto
(system) theme, and choose between English and German.
