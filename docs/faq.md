# Cookmaid — FAQ & Feature Guide

Cookmaid is a self-hosted meal planning app. This guide covers common
workflows and answers frequently asked questions.

## Recipes

### How do I add a recipe?

Tap the **+** button on the recipe list screen. Fill in the name,
description, ingredients, and steps. You can also add tags to organize
your recipes (e.g., "vegetarian", "quick", "dessert").

### How do I edit or delete a recipe?

Open the recipe detail screen and tap the edit icon. To delete a
recipe, swipe the recipe in the list or use the delete option from
the detail screen.

### Can I search for recipes?

Yes. Use the search bar at the top of the recipe list. You can search
by name or filter by tags using the tag chips below the search bar.

### How do I get a random recipe suggestion?

On the recipe list screen, tap the random button to get a random
recipe suggestion. This picks from all your recipes.

## Meal Planning

### How does the meal plan work?

The meal plan shows a weekly view. Each day has slots where you can
add recipes or free-text items. Navigate between weeks using the
arrow buttons.

### How do I add a recipe to a day?

From a recipe's detail screen, use the "Add to meal plan" option and
pick the day. You can also add items directly from the meal plan
screen.

### Can I move items between days?

Swipe an item to edit it, then change the assigned day.

## Shopping List

### How is the shopping list generated?

When you add a recipe to the meal plan, you can choose to add its
ingredients to the shopping list. Items from the catalog are matched
automatically so quantities are grouped.

### How do I check off items?

Tap an item to mark it as done. Done items move to the bottom of the
list. You can also filter the list to show only pending items.

### How do I add a custom item?

Type the item name in the input field at the top of the shopping list
screen and press enter. If a matching catalog item exists, it is used
automatically.

### How do I remove items?

Swipe an item to delete it, or use the clear option to remove all
checked items at once.

## General

### What platforms does Cookmaid support?

- **Web** — Progressive Web App (works in any modern browser, installable on desktop and mobile)
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
