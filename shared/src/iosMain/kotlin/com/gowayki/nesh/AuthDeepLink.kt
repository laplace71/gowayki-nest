package com.gowayki.nesh

import com.gowayki.nesh.app.di.AppDi
import com.gowayki.nesh.app.di.SupabaseDi
import io.github.jan.supabase.auth.handleDeeplinks
import platform.Foundation.NSURL

/**
 * Puente Swift → Kotlin para deep links OAuth (`com.gowayki.nesh://login-callback`).
 */
fun handleAuthDeepLink(urlString: String) {
    if (!AppDi.started) AppDi.start()
    val url = NSURL.URLWithString(urlString) ?: return
    SupabaseDi.client().handleDeeplinks(url)
}
