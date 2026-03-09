package io.github.fgrutsch.recipe

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val recipeModule = module {
    singleOf(::InMemoryRecipeRepository) bind RecipeRepository::class
    singleOf(::InMemoryTagRepository) bind TagRepository::class
}
