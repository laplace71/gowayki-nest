package com.gowayki.nesh.app.di

/**
 * Contenedor DI ligero (estilo GetIt de gowayki / pulse).
 * Las features resuelven dependencias aquí; no conocen `infra`.
 */
object AppDi {
    @PublishedApi
    internal val singletons = mutableMapOf<String, Any>()

    @PublishedApi
    internal val lazyFactories = mutableMapOf<String, () -> Any>()

    @Volatile
    var started: Boolean = false
        private set

    /**
     * Arranque de módulos de aplicación.
     * Llamar una vez desde el host nativo (MainActivity / MainViewController).
     */
    fun start() {
        if (started) return
        SupabaseDi.install()
        AuthDi.install()
        started = true
    }

    inline fun <reified T : Any> register(instance: T) {
        singletons[key<T>()] = instance
        lazyFactories.remove(key<T>())
    }

    inline fun <reified T : Any> registerLazy(noinline factory: () -> T) {
        lazyFactories[key<T>()] = factory
        singletons.remove(key<T>())
    }

    inline fun <reified T : Any> get(): T {
        val k = key<T>()
        singletons[k]?.let { return it as T }
        val factory = lazyFactories[k]
            ?: error("AppDi: ${T::class.simpleName} no está registrado. ¿Llamaste AppDi.start()?")
        val created = factory() as T
        singletons[k] = created
        lazyFactories.remove(k)
        return created
    }

    inline fun <reified T : Any> getOrNull(): T? =
        runCatching { get<T>() }.getOrNull()

    fun reset() {
        singletons.clear()
        lazyFactories.clear()
        started = false
    }

    @PublishedApi
    internal inline fun <reified T : Any> key(): String =
        T::class.qualifiedName ?: T::class.simpleName ?: error("Tipo sin nombre")
}
