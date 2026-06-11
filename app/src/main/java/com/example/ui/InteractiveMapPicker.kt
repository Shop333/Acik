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
    var mapZoom by remember { mutableFloatStateOf(1.2f) }
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
                        "Konfirmasi Lokasi G-Maps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Atur titik koordinat GPS pengiriman barang",
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
                            // Center viewport around match
                            mapCenterX = 500f + ((lng - 115.2625) * 1200 / mapZoom).toFloat()
                            mapCenterY = 400f - ((lat - (-8.5069)) * 1200 / mapZoom).toFloat()
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
                                // Center coordinate on viewport
                                mapCenterX = 400f
                                mapCenterY = 300f
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
                    .background(if (mapStyle == "Satelit (G-Map)") Color(0xFF1E293B) else Color(0xFFF1F5F9))
            ) {
                // Animated bounce for pin
                val infiniteTransition = rememberInfiniteTransition(label = "pin_bounce")
                val bounceOffset by infiniteTransition.animateFloat(
                    initialValue = -12f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bounce"
                )

                // Vector Map Canvas Drawing with responsive tapping
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                // Calculate coordinate from tap position relative to map center and zoom level
                                val dx = tapOffset.x - (size.width / 2f)
                                val dy = tapOffset.y - (size.height / 2f)
                                
                                // Map calculations
                                lng = 115.2625 + (dx / (300 * mapZoom))
                                lat = -8.5069 - (dy / (300 * mapZoom))
                                
                                // Generate a robust Bali-styled address
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
                ) {
                    val w = size.width
                    val h = size.height
                    
                    // Center of Bali drawing center is parent center
                    val cx = w / 2f
                    val cy = h / 2f

                    // Draw Grid
                    val gridSpacing = 40f * mapZoom
                    val gridColor = if (mapStyle == "Satelit (G-Map)") Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)
                    
                    var xVal = 0f
                    while (xVal < w) {
                        drawLine(gridColor, Offset(xVal, 0f), Offset(xVal, h), strokeWidth = 1f)
                        xVal += gridSpacing
                    }
                    var yVal = 0f
                    while (yVal < h) {
                        drawLine(gridColor, Offset(0f, yVal), Offset(w, yVal), strokeWidth = 1f)
                        yVal += gridSpacing
                    }

                    // Draw Coastlines & Roads
                    val primaryColor = if (mapStyle == "Satelit (G-Map)") Color(0xFFD97706) else Color(0xFF6366F1)
                    val waterColor = if (mapStyle == "Satelit (G-Map)") Color(0xFF0F172A) else Color(0xFFE0F2FE)
                    val landColor = if (mapStyle == "Satelit (G-Map)") Color(0xFF1E293B) else Color(0xFFF8FAFC)
                    val roadColor = if (mapStyle == "Satelit (G-Map)") Color(0xFF334155) else Color(0xFFFFFFFF)

                    // Draw ocean border
                    drawRect(color = waterColor, size = Size(w, h))

                    // Draw beautiful stylized land contour of Southern Bali
                    val landPath = Path().apply {
                        moveTo(0f, h * 0.4f)
                        quadraticTo(cx * 0.8f, cy * 0.5f, cx * 1.2f, cy * 0.2f)
                        lineTo(w, cy * 0.4f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(path = landPath, color = landColor)

                    // Draw main Bali highways (Bypass Ngurah Rai / Bypass Ida Bagus Mantra)
                    val mainHighway = Path().apply {
                        moveTo(0f, cy * 1.1f)
                        quadraticTo(cx * 0.7f, cy * 1.0f, cx * 1.2f, cy * 0.9f)
                        quadraticTo(cx * 1.5f, cy * 1.1f, w, cy * 1.2f)
                    }
                    drawPath(
                        path = mainHighway,
                        color = roadColor,
                        style = Stroke(width = 8f * mapZoom)
                    )
                    drawPath(
                        path = mainHighway,
                        color = primaryColor.copy(alpha = 0.4f),
                        style = Stroke(width = 10f * mapZoom)
                    )

                    // Draw secondary roads leading to Ubud (northwards)
                    val ubudRoad = Path().apply {
                        moveTo(cx * 1.0f, cy * 0.95f)
                        quadraticTo(cx * 0.95f, cy * 0.6f, cx * 1.05f, cy * 0.35f)
                    }
                    drawPath(
                        path = ubudRoad,
                        color = roadColor,
                        style = Stroke(width = 6f * mapZoom)
                    )

                    // Draw hub dots
                    BaliDistricts.forEach { dist ->
                        // Calculate offset coordinates
                        val xOffset = cx + ((dist.longitude - lng) * 300 * mapZoom).toFloat()
                        val yOffset = cy - ((dist.latitude - lat) * 300 * mapZoom).toFloat()

                        if (xOffset in 0f..w && yOffset in 0f..h) {
                            // Circular pulse representing the area
                            drawCircle(
                                color = primaryColor.copy(alpha = 0.15f),
                                radius = 24f * mapZoom,
                                center = Offset(xOffset, yOffset)
                            )
                            drawCircle(
                                color = primaryColor,
                                radius = 6f * mapZoom,
                                center = Offset(xOffset, yOffset)
                            )

                            // Label
                            drawText(
                                textMeasurer = textMeasurer,
                                text = dist.name,
                                topLeft = Offset(xOffset + 10f, yOffset - 16f),
                                style = TextStyle(
                                    color = if (mapStyle == "Satelit (G-Map)") Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Water Label
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "SAMUDERA HINDIA",
                        topLeft = Offset(cx * 0.3f, cy * 1.5f),
                        style = TextStyle(
                            color = if (mapStyle == "Satelit (G-Map)") Color.White.copy(alpha = 0.25f) else Color.Blue.copy(alpha = 0.25f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        )
                    )
                }

                // Stationary Target Marker at center of local view
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = bounceOffset.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.wrapContentSize()
                    ) {
                        // Coordinates preview tooltip
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .shadow(2.dp)
                        ) {
                            Text(
                                String.format("%.4f, %.4f", lat, lng),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Custom Balinese Gold pin icon representation
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Pin Peta",
                            tint = Color(0xFFD97706), // Gold Accent
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(1.dp, CircleShape)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Pin base Shadow
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 2.dp)
                        .size(16.dp, 4.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                )

                // Map control buttons (Style TOGGLE, Zoom IN, Zoom OUT, Reset Center)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            mapStyle = if (mapStyle == "Satelit (G-Map)") "Standar" else "Satelit (G-Map)"
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape,
                        modifier = Modifier.testTag("map_style_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Map Style"
                        )
                    }

                    SmallFloatingActionButton(
                        onClick = { mapZoom = (mapZoom + 0.3f).coerceAtMost(3.0f) },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape,
                        modifier = Modifier.testTag("map_zoom_in")
                    ) {
                        Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }

                    SmallFloatingActionButton(
                        onClick = { mapZoom = (mapZoom - 0.3f).coerceAtLeast(0.6f) },
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
                        "Google Maps API 3D Active",
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
