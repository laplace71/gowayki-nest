// Persistencia del modo oscuro (sobrevive al cerrar la app).
// Sin librerías nuevas: SharedPreferences en Android / NSUserDefaults en iOS.
package com.gowayki.nesh.app.features.settings.data

expect object ThemeStore {
    fun isDark(): Boolean
    fun setDark(value: Boolean)
}
