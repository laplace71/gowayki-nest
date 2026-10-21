// Actual iOS — fallback OSM con el mismo calor (Mapbox iOS pediría SPM + token;
// se activa cuando lo pidas). Pin CENTRADO sobre su coordenada.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.sos.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gowayki.nesh.app.features.reports.domain.model.OsmMarkerType
import com.gowayki.nesh.app.features.reports.presentation.StateFlow.ReportsViewModel
import com.gowayki.nesh.app.features.reports.presentation.components.OsmMap
import com.gowayki.nesh.app.features.reports.presentation.components.OsmMarker
import compose.icons.FeatherIcons
import compose.icons.feathericons.MapPin
import compose.icons.feathericons.Zap

@Composable
actual fun SosHeatMap(
    markers: List<OsmMarker>,
    selectedId: String?,
    reportsVm: ReportsViewModel,
    onMarkerClick: (OsmMarker) -> Unit,
    modifier: Modifier,
) {
    val heat = remember(markers) { markers.map { it.pos } }
    OsmMap(
        state = reportsVm.map,
        tiles = reportsVm.tiles,
        markers = markers,
        selectedId = selectedId,
        route = emptyList(),
        heatPoints = heat,
        grayScale = false,
        onNeedTiles = reportsVm::ensureTiles,
        onMarkerClick = onMarkerClick,
        // Caja de 64dp desplazada -32dp: el pin queda CENTRADO en el punto.
        markerContent = { m, sel ->
            Box(Modifier.offset((-32).dp, (-32).dp)) { IosSosPin(m.type, sel) }
        },
        modifier = modifier,
    )
}

@Composable
private fun IosSosPin(type: OsmMarkerType, selected: Boolean) {
    val bg = when (type) {
        OsmMarkerType.EMERGENCIA -> Color(0xFFC64049)
        OsmMarkerType.UNIDAD -> Color(0xFF1A1A1A)
        OsmMarkerType.INCIDENCIA -> Color(0xFFB07D4F)
    }
    val size = if (selected) 46.dp else 36.dp
    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(size).background(bg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (type == OsmMarkerType.EMERGENCIA) FeatherIcons.Zap else FeatherIcons.MapPin,
                null, tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
