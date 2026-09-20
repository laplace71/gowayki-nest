package com.gowayki.nesh.infra.auth

import android.content.Intent
import com.gowayki.nesh.app.di.SupabaseDi
import io.github.jan.supabase.auth.handleDeeplinks

fun handleAndroidAuthDeepLink(intent: Intent?) {
    if (intent == null) return
    SupabaseDi.client().handleDeeplinks(intent)
}
