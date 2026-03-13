package io.github.fgrutsch.ui.mealplan

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val mealPlanModule = module {
    factoryOf(::MealPlanViewModel)
}
