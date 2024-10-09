package com.fgrutsch.cookmaid

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform