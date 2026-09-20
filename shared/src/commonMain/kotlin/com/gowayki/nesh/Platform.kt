package com.gowayki.nesh

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform