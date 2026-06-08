package io.github.fgrutsch.cookmaid.ui.user

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val userModule = module {
    singleOf(::ApiUserClient) bind UserClient::class
}
