package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

data class BaliLocation(
    val name: String,
    val regency: String,
    val latitude: Double,
    val longitude: Double,
    val address: String
)

val BaliDistricts = listOf(
    BaliLocation("Ubud Center", "Gianyar", -8.5069, 115.2625, "Jl. Raya Ubud No. 12, Kelurahan Padangtegal, Ubud, Gianyar"),
    BaliLocation("Denpasar City Hub", "Denpasar", -8.6705, 115.2126, "Jl. Diponegoro No. 88, Dauh Puri Klod, Denpasar Barat, Denpasar"),
    BaliLocation("Seminyak Coast", "Badung", -8.6913, 115.1682, "Jl. Camplung Tanduk No. 15, Seminyak, Kuta, Badung"),
    BaliLocation("Kuta Beach Market", "Badung", -8.7392, 115.1711, "Jl. Pantai Kuta No. 45, Kuta, Badung"),
    BaliLocation("Sanur Harbor", "Denpasar", -8.6756, 115.2638, "Jl. Hang Tuah No. 220, Sanur Kaja, Denpasar Selatan, Denpasar"),
    BaliLocation("Candidasa Village", "Karangasem", -8.4902, 115.5619, "Jl. Raya Candidasa No. 9, Sengkidu, Manggis, Karangasem")
)

@OptIn(ExperimentalTextApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InteractiveMapPicker(
    onLocationSelected: (address: String, lat: Double, lng: Double) -> Unit,
    onDismiss: () -> Unit,
    initialLat: Double = -8.5069,
    initialLng: Double = 115.2625,
    initialAddress: String = ""
) {
    var lat by remember { mutableStateOf(initialLat) }
    var lng by remember { mutableStateOf(initialLng) }
    var address by remember { mutableStateOf(if (initialAddress.isEmpty()) BaliDistricts[0].address else initialAddress) }
    var mapZoom by remember { mutableDoubleStateOf(14.0) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Custom viewport coordinate panning simulation
    var mapCenterX by remember { mutableFloatStateOf(500f) }
    var mapCenterY by remember { mutableFloatStateOf(400f) }

    // Map style (Satellite, Terrain, Standard)
    var mapStyle by remember { mutableStateOf("Satelit (G-Map)") }

    // Text Measurer for visual labels
    val textMeasurer = rememberTextMeasurer()

    Card(
        modifier = Modifier
            .fillMaxSize()
            .testTag("interactive_map_dialog"),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "Konfirmasi Lokasi OSM Map",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Atur titik koordinat GPS pengiriman barang secara gratis",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "My Location",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Quick search & Districts list
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        val matched = BaliDistricts.firstOrNull { it.name.contains(query, ignoreCase = true) || it.regency.contains(query, ignoreCase = true) }
                        if (matched != null) {
                            lat = matched.latitude
                            lng = matched.longitude
                            address = matched.address
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("map_search_input"),
                    placeholder = { Text("Cari lokasi di Bali (contoh: Ubud, Sanur)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bali Districts Quick-Teleport Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(BaliDistricts) { location ->
                        val isSelected = (location.latitude == lat && location.longitude == lng)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                lat = location.latitude
                                lng = location.longitude
                                address = location.address
                            },
                            label = { Text(location.name, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("chip_${location.name.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }

            // Map View Simulator Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                // Real OpenStreetMap view integration using org.osmdroid.views.MapView
                AndroidView(
                    factory = { ctx ->
                        // Configure OSM user-agent
                        Configuration.getInstance().userAgentValue = ctx.packageName
                        
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                            
                            // Center on start position
                            controller.setZoom(mapZoom)
                            val startPoint = GeoPoint(lat, lng)
                            controller.setCenter(startPoint)

                            // Initial draggable Marker for shipment location
                            val marker = Marker(this).apply {
                                position = startPoint
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = "Geser penanda untuk ubah alamat"
                                isDraggable = true
                                setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                                    override fun onMarkerDrag(m: Marker?) {}
                                    override fun onMarkerDragStart(m: Marker?) {}
                                    override fun onMarkerDragEnd(m: Marker?) {
                                        m?.let {
                                            lat = it.position.latitude
                                            lng = it.position.longitude
                                            
                                            // Simulated local reverse-geocoding of Bali address based on updated coordinates
                                            val rId = (lat.absoluteValue * 1000 + lng * 1000).roundToInt()
                                            address = when {
                                                lat > -8.55 -> "Jl. Raya Ubud No. ${rId % 80 + 1}, Padangtegal, Ubud, Gianyar"
                                                lat < -8.65 && lng > 115.24 -> "Jl. Bypass Ngurah Rai No. ${rId % 100 + 10}, Sanur, Kota Denpasar"
                                                lat < -8.65 && lng <= 115.24 -> "Jl. Suniaraja No. ${rId % 150 + 2}, Dauh Puri Kangin, Kota Denpasar"
                                                lng < 115.18 -> "Jl. Sunset Road Raya No. ${rId % 200 + 40}, Seminyak, Kuta, Badung"
                                                else -> "Jl. Raya Uluwatu No. ${rId % 100 + 20}, Jimbaran, Kuta Selatan, Badung"
                                            }
                                        }
                                    }
                                })
                            }
                            overlays.add(marker)
                        }
                    },
                    update = { mv ->
                        val currentCenter = mv.mapCenter
                        if ((currentCenter.latitude - lat).absoluteValue > 0.0001 || 
                            (currentCenter.longitude - lng).absoluteValue > 0.0001) {
                            val geoPoint = GeoPoint(lat, lng)
                            mv.controller.animateTo(geoPoint)
                            
                            // Re-sync markers
                            mv.overlays.filterIsInstance<Marker>().forEach { m ->
                                m.position = geoPoint
                            }
                        }
                        if (mv.zoomLevelDouble != mapZoom) {
                            mv.controller.setZoom(mapZoom)
                        }
                        mv.invalidate()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("osm_map_view")
                )

                // Map control buttons (Zoom IN, Zoom OUT, Reset Center)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = { mapZoom = (mapZoom + 1.0).coerceAtMost(20.0) },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape,
                        modifier = Modifier.testTag("map_zoom_in")
                    ) {
                        Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }

                    SmallFloatingActionButton(
                        onClick = { mapZoom = (mapZoom - 1.0).coerceAtLeast(4.0) },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape,
                        modifier = Modifier.testTag("map_zoom_out")
                    ) {
                        Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }

                // GPS Signal / Info Tag
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.Green, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "OpenStreetMap Active (100% Free)",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Bottom Selected Address & Action Panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Alamat Terpilih",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Alamat Pengiriman Terpilih",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row showing Latitude and Longitude
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("LATITUDE", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(String.format("%.6f", lat), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Column {
                            Text("LONGITUDE", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(String.format("%.6f", lng), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onLocationSelected(address, lat, lng) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("confirm_location_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Pilih")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gunakan Alamat Ini", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
