package io.github.fgrutsch.mealplan

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val mealPlanModule = module {
    singleOf(::InMemoryMealPlanRepository) bind MealPlanRepository::class
}
