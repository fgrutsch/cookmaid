package io.github.fgrutsch.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.ShoppingList::class, Route.ShoppingList.serializer())
            subclass(Route.RecipeList::class, Route.RecipeList.serializer())
            subclass(Route.MealPlan::class, Route.MealPlan.serializer())
            subclass(Route.Settings::class, Route.Settings.serializer())
            subclass(Route.RecipeDetail::class, Route.RecipeDetail.serializer())
            subclass(Route.AddRecipe::class, Route.AddRecipe.serializer())
            subclass(Route.EditRecipe::class, Route.EditRecipe.serializer())
        }
    }
}
