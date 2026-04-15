package io.github.fgrutsch.cookmaid.ui.mealplan

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val mealPlanModule = module {
    singleOf(::ApiMealPlanClient) bind MealPlanClient::class
    singleOf(::ApiMealPlanRepository) bind MealPlanRepository::class
    singleOf(::MealPlanViewModel)
    factoryOf(::DayPickerViewModel)
}
