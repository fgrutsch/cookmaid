package io.github.fgrutsch.cookmaid.common.llm

import org.koin.dsl.module

val llmModule = module {
    single { LlmConfig.from(get()) }
}
