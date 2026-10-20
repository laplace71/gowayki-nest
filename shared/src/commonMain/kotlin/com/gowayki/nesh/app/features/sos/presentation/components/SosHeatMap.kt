// Mapa SOS (Mapbox en Android, OSM en iOS) con markers de emergencia.
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.sos.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gowayki.nesh.app.features.reports.presentation.StateFlow.ReportsViewModel
import com.gowayki.nesh.app.features.reports.presentation.components.OsmMarker

expect @Composable fun SosHeatMap(
    markers: List<OsmMarker>,
    selectedId: String?,
    reportsVm: ReportsViewModel,
    onMarkerClick: (OsmMarker) -> Unit,
    modifier: Modifier,
)