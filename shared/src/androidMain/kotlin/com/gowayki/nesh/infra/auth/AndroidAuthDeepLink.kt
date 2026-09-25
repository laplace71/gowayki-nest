package com.gowayki.nesh.infra.auth

import android.content.Intent

// DORMIDO — Deep link OAuth de Supabase.
// Fase JSON/fake: no-op. Para reactivar (con SupabaseClient):
//   SupabaseDi.client().handleDeeplinks(intent)
// y volver a llamar handleAndroidAuthDeepLink desde MainActivity.
fun handleAndroidAuthDeepLink(intent: Intent?) {
    if (intent == null) return
    // no-op en fase JSON (sin SupabaseClient)
}