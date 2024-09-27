package com.glitch.securenotes.domain.utils.codeauth

import io.ktor.websocket.*

data class CodeAuthMember(
    val code: String,
    val socketSession: WebSocketSession
)
