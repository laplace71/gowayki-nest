package com.gowayki.nesh.app.common.ui.shared

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow

/**
 * `true` mientras el usuario se desplaza hacia arriba (o la lista está
 * arriba del todo). Se usa para esconder los FAB al bajar y mostrarlos al
 * subir: menos ruido y más contenido visible, como en las apps modernas.
 *
 * Usa `snapshotFlow` en lugar de `derivedStateOf` con estado mutable, así que
 * no se reevalúa en cada frame ni dispara recomposiciones en cascada. Aun así,
 * conviene leerlo dentro del composable más pequeño posible (el propio FAB).
 */
@Composable
fun LazyListState.isScrollingUp(): Boolean {
    var visible by remember(this) { mutableStateOf(true) }
    LaunchedEffect(this) {
        var previousIndex = firstVisibleItemIndex
        var previousOffset = firstVisibleItemScrollOffset
        snapshotFlow { firstVisibleItemIndex to firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val up = if (index != previousIndex) {
                    index < previousIndex
                } else {
                    offset <= previousOffset
                }
                previousIndex = index
                previousOffset = offset
                // Arriba del todo siempre se muestra.
                visible = up || (index == 0 && offset == 0)
            }
    }
    return visible
}
