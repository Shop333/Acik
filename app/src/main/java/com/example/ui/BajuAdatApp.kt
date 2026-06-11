package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.Product
import com.example.data.Transaction
import com.example.viewmodel.BajuAdatViewModel
import com.example.viewmodel.CartProductItem
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun BajuAdatApp(viewModel: BajuAdatViewModel = viewModel()) {
    val currentUser by viewModel.currentUser.collectAsState()
    var currentTab by remember { mutableStateOf("Beranda") }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            currentTab = "Beranda"
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentUser != null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.shadow(16.dp)
                ) {
                    val tabs = listOf(
                        Triple("Beranda", Icons.Default.Storefront, "Beranda"),
                        Triple("Keranjang", Icons.Default.ShoppingCart, "Keranjang"),
                        Triple("Status", Icons.Default.LocalShipping, "Status"),
                        Triple("Profil", Icons.Default.Person, "Profil")
                    )
                    tabs.forEach { (tabName, icon, label) ->
                        val isSelected = currentTab == tabName
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tabName },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_tab_${tabName.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (currentUser == null) {
                AuthScreen(viewModel)
            } else {
                when (currentTab) {
                    "Beranda" -> BerandaScreen(viewModel)
                    "Keranjang" -> KeranjangScreen(viewModel)
                    "Status" -> StatusScreen(viewModel)
                    "Profil" -> ProfilScreen(viewModel, onNavigateToBeranda = { currentTab = "Beranda" })
                }
            }
        }
    }
}

// ----------------- AUTHENTICATION VIEW -----------------

@Composable
fun AuthScreen(viewModel: BajuAdatViewModel) {
    var isLoginTab by remember { mutableStateOf(true) }
    val authError by viewModel.authError.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Set up Google Sign-In options & launcher
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.loginWithGoogle(idToken)
            } else {
                viewModel.setAuthError("Token ID Google tidak ditemukan!")
            }
        } catch (e: Exception) {
            viewModel.setAuthError("Gagal Google Sign-In: ${e.localizedMessage}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Large Brand Icon vector representing Balinese Temple Gate Candi Bentar
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFD69E2E), Color(0xFF1A365D))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .shadow(8.dp, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(64.dp)) {
                val path = Path().apply {
                    // Left gate pillar
                    moveTo(size.width * 0.25f, size.height * 0.9f)
                    lineTo(size.width * 0.25f, size.height * 0.2f)
                    lineTo(size.width * 0.4f, size.height * 0.35f)
                    lineTo(size.width * 0.4f, size.height * 0.9f)
                    close()

                    // Right gate pillar
                    moveTo(size.width * 0.75f, size.height * 0.9f)
                    lineTo(size.width * 0.75f, size.height * 0.2f)
                    lineTo(size.width * 0.6f, size.height * 0.35f)
                    lineTo(size.width * 0.6f, size.height * 0.9f)
                    close()
                }
                drawPath(path = path, color = Color.White)
                drawCircle(color = Color(0xFFFDE68A), radius = 6f, center = center)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "BajuAdat Bali",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Busana Tradisional Dewata Premium",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Tab switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            Button(
                onClick = { isLoginTab = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("auth_tab_login"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLoginTab) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (isLoginTab) Color.White else Color.Gray
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = null
            ) {
                Text("Masuk", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { isLoginTab = false },
                modifier = Modifier
                    .weight(1f)
                    .testTag("auth_tab_register"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isLoginTab) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (!isLoginTab) Color.White else Color.Gray
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = null
            ) {
                Text("Daftar Akun", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Animated input cards for smooth sliding / crossfade transitions
        AnimatedContent(
            targetState = isLoginTab,
            transitionSpec = {
                (fadeIn(animationSpec = tween(300)) + slideInHorizontally(animationSpec = tween(300), initialOffsetX = { if (targetState) -it else it }))
                    .togetherWith(fadeOut(animationSpec = tween(300)) + slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { if (targetState) it else -it }))
            },
            label = "auth_form_transition",
            modifier = Modifier.fillMaxWidth()
        ) { isLoginState ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!isLoginState) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Nama Lengkap") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_auth_name"),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isAuthLoading,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("No. Whatsapp / Telpon") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_auth_phone"),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isAuthLoading,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_auth_email"),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isAuthLoading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Sandi") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_auth_password"),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isAuthLoading,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        singleLine = true
                    )
                }
            }
        }

        authError?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main action execution button with loading integration spinner
        Button(
            onClick = {
                if (isLoginTab) {
                    viewModel.loginUser(email, password)
                } else {
                    viewModel.registerUser(email, password, fullName, phone)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("submit_auth_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(26.dp),
            enabled = !isAuthLoading
        ) {
            if (isAuthLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    if (isLoginTab) "Masuk Sekarang" else "Daftar Akun Baru",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f))
            Text(
                "atau",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Decorative Google Sign-In Button
        OutlinedButton(
            onClick = {
                googleSignInClient.signOut().addOnCompleteListener {
                    val signInIntent = googleSignInClient.signInIntent
                    googleSignInLauncher.launch(signInIntent)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(2.dp, RoundedCornerShape(26.dp))
                .testTag("google_auth_button"),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(26.dp),
            enabled = !isAuthLoading
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Colored G shape vector drawing
                Canvas(modifier = Modifier.size(20.dp)) {
                    val w = size.width
                    val h = size.height
                    
                    drawArc(
                        color = Color(0xFFEA4335),
                        startAngle = 135f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset.Zero,
                        size = size
                    )
                    drawArc(
                        color = Color(0xFFFBBC05),
                        startAngle = 45f,
                        sweepAngle = 90f,
                        useCenter = true,
                        topLeft = Offset.Zero,
                        size = size
                    )
                    drawArc(
                        color = Color(0xFF34A853),
                        startAngle = -45f,
                        sweepAngle = 90f,
                        useCenter = true,
                        topLeft = Offset.Zero,
                        size = size
                    )
                    drawArc(
                        color = Color(0xFF4285F4),
                        startAngle = -135f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset.Zero,
                        size = size
                    )
                    
                    drawCircle(
                        color = Color.White,
                        radius = w * 0.3f,
                        center = center
                    )
                    
                    drawRect(
                        color = Color(0xFF4285F4),
                        topLeft = Offset(w * 0.5f, h * 0.4f),
                        size = Size(w * 0.4f, h * 0.2f)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Masuk dengan Google",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color.DarkGray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            if (isLoginTab) "Belum punya akun? Ganti tab ke Daftar" else "Sudah punya akun? Ganti tab ke Masuk",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.clickable(enabled = !isAuthLoading) {
                isLoginTab = !isLoginTab
            }
        )
    }
}

// ----------------- CATALOG VIEW (BERANDA) -----------------

@Composable
fun BerandaScreen(viewModel: BajuAdatViewModel) {
    val products by viewModel.products.collectAsState()
    val curUser by viewModel.currentUser.collectAsState()
    var selectedCategory by remember { mutableStateOf("Semua") }
    var selectedProductForDetail by remember { mutableStateOf<Product?>(null) }
    var showAiAssistantSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.startProductsListener()
        viewModel.seedDatabaseIfEmpty()
    }

    val filteredProducts = remember(products, selectedCategory) {
        if (selectedCategory == "Semua") products else products.filter { it.category == selectedCategory }
    }

    // Handles native hardware back button to safely dismiss detail overlay
    if (selectedProductForDetail != null) {
        BackHandler {
            selectedProductForDetail = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Upper Greeting Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Om Swastyastu 🙏",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Hai, ${curUser?.fullName ?: "Pembeli"}!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                
                // AI Styling Companion Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFEF3C7))
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(16.dp))
                        .clickable { showAiAssistantSheet = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("ai_assistant_trigger"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Tanya AI",
                            fontSize = 11.sp,
                            color = Color(0xFFB45309),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Horizontal Promo Scroll Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(
                            id = com.example.R.drawable.img_banner_galungan_1781020369856
                        ),
                        contentDescription = "Galungan Special Promo Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    
                    // Dark horizontal gradient scrim so text stays highly accessible and legible
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.75f),
                                        Color.Black.copy(alpha = 0.2f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("GALUNGAN SPECIAL", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Diskon Khusus 15%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Lengkap Udeng, Kamen, & Kebaya premium.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // Category Selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf("Semua", "Pria", "Wanita")
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_${cat.lowercase()}")
                    )
                }
            }

            // Product Catalog Layout
            if (filteredProducts.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Memuat busana adat...", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProducts) { prod ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedProductForDetail = prod }
                                .testTag("product_card_${prod.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column {
                                // Product Visual vector
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val resId = getProductImageResource(prod.imageUrl)
                                    if (resId != null) {
                                        androidx.compose.foundation.Image(
                                            painter = androidx.compose.ui.res.painterResource(id = resId),
                                            contentDescription = prod.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        ProductImagePlaceholder(imageName = prod.imageUrl, category = prod.category)
                                    }
                                    
                                    // Tag category top right
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .background(
                                                if (prod.category == "Pria") Color(0xFF1E3A8A) else Color(0xFF831843),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(prod.category, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        prod.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        String.format("Rp %,.0f", prod.price),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Inventory,
                                            contentDescription = "Stok",
                                            modifier = Modifier.size(11.dp),
                                            tint = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Stok: ${prod.stock} pcs",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Custom Immersive Detail Overlay (Sliding Sheet)
        AnimatedVisibility(
            visible = selectedProductForDetail != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            selectedProductForDetail?.let { prod ->
                ProductDetailScreen(
                    product = prod,
                    viewModel = viewModel,
                    onDismiss = { selectedProductForDetail = null }
                )
            }
        }

        // Tanya AI Styling Dialog
        if (showAiAssistantSheet) {
            AiAssistantDialog(
                viewModel = viewModel,
                onDismiss = { showAiAssistantSheet = false }
            )
        }
    }
}

// ----------------- DETAILED PRODUCT VIEW -----------------

data class DetailedSpecs(
    val material: String,
    val pattern: String,
    val cultureDetail: String,
    val tips: String
)

fun getProductDetails(imageUrl: String): DetailedSpecs {
    return when (imageUrl) {
        "pria_safari" -> DetailedSpecs(
            material = "Premium Katun Toyobo (Sangat sejuk & rapi)",
            pattern = "Polos Formal dengan saku ganda dada khas adat",
            cultureDetail = "Warna putih melambangkan kesucian pikiran (Niyasa) untuk beribadah ke Pura atau menghadiri upacara sakral adat Bali.",
            tips = "Sangat direkomendasikan dipadukan dengan Udeng putih polos serta Saput Poleng untuk memperkuat wibawa."
        )
        "pria_kamen_songket" -> DetailedSpecs(
            material = "Tenun Tangan Songket Sutra Bali",
            pattern = "Bunga Khas Klungkung (Sulaman Benang Emas)",
            cultureDetail = "Kamen Songket melambangkan kehormatan besar dan status agung keluarga. Umumnya dipakai pada hari perkawinan (Pawiwahan) atau upacara besar kemasyarakatan.",
            tips = "Hindari merendam kain. Cukup angin-anginkan di area teduh agar benang emas jacquard tetap bercahaya."
        )
        "pria_udeng" -> DetailedSpecs(
            material = "Katun Prima Istimewa",
            pattern = "Putih Agung Tradisional Bali",
            cultureDetail = "Ikat kepala ini melambangkan pemusatan akal dan budi pekerti luhur (Ngiket) guna mengekang ego hewani saat menghadap Tuhan.",
            tips = "Simpan dalam keadaan tergantung dengan kapur barus agar bentuk simpul khasnya tidak kusut."
        )
        "pria_saput" -> DetailedSpecs(
            material = "Tenun Klasik Semi Sutra",
            pattern = "Poleng Kotak-Kotak (Rua Bhineda)",
            cultureDetail = "Corak sakral hitam-putih melambangkan dualitas alam semesta yang seimbang: malam-siang, suka-duka, pengingat mawas diri.",
            tips = "Dipasang rapi menyelimuti kamen bawah dengan menyisakan ujung kain merah menyembul gagah."
        )
        "wanita_kebaya" -> DetailedSpecs(
            material = "Brocade Soft Chantilly (Sangat lentur & jatuh lurus)",
            pattern = "Bunga Rambat Brokat Khas Bali",
            cultureDetail = "Sangat anggun dan merefleksikan kepatuhan, keibuan, serta kesucian batin wanita Bali dalam menunaikan persembahyangan di Pura.",
            tips = "Padukan dengan dalaman korset senada dan ikat menggunakan selendang (angkin) berwarna kontras."
        )
        "wanita_kamen_prada" -> DetailedSpecs(
            material = "Sutra Jacquard dengan Sablon Prada Emas",
            pattern = "Bunga Patra & Rebung Emas Maroon",
            cultureDetail = "Warna merah marun dipadu emas berkilau melambangkan kemakmuran, gairah spiritual, dan keberkahan bumi Bali yang berlimpah.",
            tips = "Cukup bersihkan noda kecil secara lokal menggunakan spons basah agar detail prada emas prada awet."
        )
        "wanita_selendang" -> DetailedSpecs(
            material = "Sutra Sifon Premium Lembut",
            pattern = "Polos Cantik bertekstur lipit halus",
            cultureDetail = "Ikat pinggang adat (angkin) mengisyaratkan pengikatan nafsu liar tubuh dan pengendalian organ reproduksi sebelum memasuki tanah suci tempat peribadatan.",
            tips = "Pelintir atau ikat erat di sebelah perut kiri depan dengan juntaian kain simetris memikat."
        )
        "wanita_sanggul" -> DetailedSpecs(
            material = "Serat Rambut Sintetis Murni + Kelopak Kamboja Alami",
            pattern = "Sanggul Bali Pusung Tagel Klasik",
            cultureDetail = "Tatanan rambut formal pusung tagel dihiasi bunga jepun (kamboja) melambangkan kematangan pikiran serta kehormatan wanita dalam tradisi Bali.",
            tips = "Sematkan bobby pin tambahan di sisi sanggul agar tetap nyaman sepanjang upacara berlangsung."
        )
        else -> DetailedSpecs(
            material = "Bahan Pilihan Terbaik Adat Dewata",
            pattern = "Motif Ornamen Budaya Warisan Tradisional",
            cultureDetail = "Busana adat Bali sarat makna religius tinggi dalam menghidupkan khazanah warisan luhur para leluhur.",
            tips = "Gunakan secara rapi untuk menghormati kehormatan perayaan tradisi."
        )
    }
}

fun getProductImageResource(imageUrl: String): Int? {
    return when (imageUrl) {
        "pria_safari" -> com.example.R.drawable.img_safari_putih_1781017985055
        "pria_kamen_songket" -> com.example.R.drawable.img_kamen_songket_1781018002389
        "pria_udeng" -> com.example.R.drawable.img_udeng_putih_1781020387662
        "pria_saput" -> com.example.R.drawable.img_saput_poleng_1781020407386
        "wanita_kebaya" -> com.example.R.drawable.img_kebaya_kuning_1781018022177
        "wanita_kamen_prada" -> com.example.R.drawable.img_kamen_prada_1781018039009
        "wanita_selendang" -> com.example.R.drawable.img_selendang_bali_1781020422846
        "wanita_sanggul" -> com.example.R.drawable.img_sanggul_bali_1781020439921
        else -> null
    }
}

@Composable
fun ProductDetailScreen(
    product: Product,
    viewModel: BajuAdatViewModel,
    onDismiss: () -> Unit
) {
    val specs = remember(product.imageUrl) { getProductDetails(product.imageUrl) }
    var quantity by remember { mutableIntStateOf(1) }
    var isAddedToCartSuccessfully by remember { mutableStateOf(false) }

    // High-resolution photo asset loader resolver
    val resId = remember(product.imageUrl) { getProductImageResource(product.imageUrl) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Scrollable content body details
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Upper full-bleed images section height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            ) {
                if (resId != null) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = resId),
                        contentDescription = product.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("detail_high_res_image"),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    // Fallback visual representations inside details
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ProductImagePlaceholder(imageName = product.imageUrl, category = product.category)
                    }
                }

                // High-fidelity dark overlay vignettes
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        )
                )

                // Category badges bottom left
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .background(
                            if (product.category == "Pria") Color(0xFF1E3A8A) else Color(0xFF831843),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Busana ${product.category}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Technical specs and descriptions content card info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Product Name Header
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    modifier = Modifier.testTag("detail_product_name")
                )

                // Prices and Stock Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("Rp %,.0f", product.price),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        modifier = Modifier.testTag("detail_product_price")
                    )

                    // Stock availability indicators
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (product.stock > 0) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (product.stock > 0) Color(0xFF10B981) else Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (product.stock > 0) "Stok: ${product.stock} pcs" else "Habis",
                            color = if (product.stock > 0) Color(0xFF065F46) else Color(0xFF991B1B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Descriptions Sections
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Deskripsi Produk",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        lineHeight = 22.sp,
                        modifier = Modifier.testTag("detail_product_description")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Cultural Stories Card (Philosophy)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Makna Adat",
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Makna Filosofis & Nilai Budaya",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                        Text(
                            text = specs.cultureDetail,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Technical specification detail tables
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Spesifikasi Busana Adat",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // Material Row spec
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Bahan",
                                modifier = Modifier.width(80.dp),
                                fontSize = 13.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = specs.material,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Pattern Row spec
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Corak/Motif",
                                modifier = Modifier.width(80.dp),
                                fontSize = 13.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = specs.pattern,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Recommendation and usage guidelines tips
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Saran Pakai",
                                modifier = Modifier.width(80.dp),
                                fontSize = 13.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = specs.tips,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Custom purchases quantities selector box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Jumlah Pembelian",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Tentukan banyaknya pcs",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            enabled = product.stock > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Kurang",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.testTag("detail_quantity_text")
                        )

                        IconButton(
                            onClick = { if (quantity < product.stock) quantity++ },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            enabled = product.stock > 0 && quantity < product.stock
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Prevent overlays at the bottom due to floating buttons
                Spacer(modifier = Modifier.height(120.dp))
            }
        }

        // Floating Back button round overlaps top-left
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .size(44.dp)
                .shadow(6.dp, CircleShape)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                .align(Alignment.TopStart)
                .testTag("detail_back_button")
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Kembali",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Floating Bottom Checkout Card row
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(12.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Harga",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format("Rp %,.0f", product.price * quantity),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        modifier = Modifier.testTag("detail_total_price")
                    )
                }

                if (isAddedToCartSuccessfully) {
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("detail_success_add_cart")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Berhasil", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Berhasil!", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            if (product.stock > 0) {
                                viewModel.addToCart(product, quantity)
                                isAddedToCartSuccessfully = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("detail_add_to_cart_button"),
                        enabled = product.stock > 0
                    ) {
                        Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = "Add", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Tambah ke Keranjang", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Auto dismissal LaunchedEffect for adding success
        if (isAddedToCartSuccessfully) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1200)
                isAddedToCartSuccessfully = false
                onDismiss()
            }
        }
    }
}

// ----------------- PRODUCT VISUAL REPRESENTATION -----------------

@Composable
fun ProductImagePlaceholder(imageName: String, category: String) {
    Canvas(
        modifier = Modifier
            .size(72.dp)
            .padding(4.dp)
    ) {
        val w = size.width
        val h = size.height

        when (imageName) {
            "pria_safari" -> {
                // Draw Safari Shirt
                val p = Path().apply {
                    moveTo(w * 0.2f, h * 0.2f)
                    lineTo(w * 0.4f, h * 0.1f)
                    lineTo(w * 0.6f, h * 0.1f)
                    lineTo(w * 0.8f, h * 0.2f)
                    lineTo(w * 0.85f, h * 0.4f)
                    lineTo(w * 0.72f, h * 0.43f)
                    lineTo(w * 0.72f, h * 0.9f)
                    lineTo(w * 0.28f, h * 0.9f)
                    lineTo(w * 0.28f, h * 0.43f)
                    lineTo(w * 0.15f, h * 0.4f)
                    close()
                }
                drawPath(p, Color.White)
                drawPath(p, Color(0xFF78111A), style = Stroke(width = 3f))
                // Draw buttons line
                drawLine(Color(0xFF78111A), Offset(w * 0.5f, h * 0.25f), Offset(w * 0.5f, h * 0.8f), strokeWidth = 2f)
                drawCircle(Color(0xFFD97706), radius = 3f, center = Offset(w * 0.5f, h * 0.35f))
                drawCircle(Color(0xFFD97706), radius = 3f, center = Offset(w * 0.5f, h * 0.5f))
                drawCircle(Color(0xFFD97706), radius = 3f, center = Offset(w * 0.5f, h * 0.65f))
            }
            "pria_kamen_songket" -> {
                // Kamen (Traditional wrap) Gold-Pattern
                drawRect(Color(0xFF312E81), size = Size(w, h))
                // Draw horizontal gold weaving lines represent BaliSongket
                var yOffset = 10f
                while (yOffset < h) {
                    drawLine(Color(0xFFFCD34D), Offset(0f, yOffset), Offset(w, yOffset), strokeWidth = 1.5f)
                    yOffset += 14f
                }
                // Diamond shape gold ornaments in center
                val dp = Path().apply {
                    moveTo(w * 0.5f, h * 0.3f)
                    lineTo(w * 0.75f, h * 0.5f)
                    lineTo(w * 0.5f, h * 0.7f)
                    lineTo(w * 0.25f, h * 0.5f)
                    close()
                }
                drawPath(dp, Color(0xFFD97706))
            }
            "pria_udeng" -> {
                // Balinese folded Headwear (Udeng)
                val udengPath = Path().apply {
                    moveTo(w * 0.1f, h * 0.6f)
                    quadraticTo(w * 0.5f, h * 0.4f, w * 0.9f, h * 0.6f)
                    lineTo(w * 0.9f, h * 0.75f)
                    lineTo(w * 0.1f, h * 0.75f)
                    close()
                }
                drawPath(udengPath, Color.White)
                drawPath(udengPath, Color(0xFFD97706), style = Stroke(width = 2.5f))
                // Bow tie center fold decoration
                val centerFold = Path().apply {
                    moveTo(w * 0.5f, h * 0.5f)
                    lineTo(w * 0.42f, h * 0.3f)
                    lineTo(w * 0.58f, h * 0.3f)
                    close()
                }
                drawPath(centerFold, Color.White)
                drawPath(centerFold, Color(0xFFD97706), style = Stroke(width = 2.5f))
            }
            "pria_saput" -> {
                // Poleng Checkered design (Balinese black & white grid)
                drawRect(Color.White, size = Size(w, h))
                val boxW = w / 4f
                val boxH = h / 4f
                for (r in 0 until 4) {
                    for (c in 0 until 4) {
                        if ((r + c) % 2 == 1) {
                            drawRect(
                                color = Color.DarkGray,
                                topLeft = Offset(c * boxW, r * boxH),
                                size = Size(boxW, boxH)
                            )
                        }
                    }
                }
                // Border red sash accent
                drawRect(Color(0xFF78111A), topLeft = Offset(0f, h * 0.82f), size = Size(w, h * 0.18f))
            }
            "wanita_kebaya" -> {
                // Yellow Brocade lace kebaya
                val body = Path().apply {
                    moveTo(w * 0.25f, h * 0.15f)
                    lineTo(w * 0.75f, h * 0.15f)
                    lineTo(w * 0.8f, h * 0.85f)
                    lineTo(w * 0.2f, h * 0.85f)
                    close()
                }
                drawPath(body, Color(0xFFFEF08A))
                drawPath(body, Color(0xFFB45309), style = Stroke(width = 2.5f))
                
                // Draw a beautiful waist ribbon bow wrap
                drawRect(Color(0xFFDC2626), topLeft = Offset(w * 0.22f, h * 0.53f), size = Size(w * 0.56f, h * 0.12f))
                drawCircle(Color(0xFFFCD34D), radius = 5f, center = Offset(w * 0.5f, h * 0.59f))
            }
            "wanita_kamen_prada" -> {
                // Maroon gold prada fabric
                drawRect(Color(0xFF580310), size = Size(w, h))
                // Golden random prada floral sparkles
                drawCircle(Color(0xFFFCD34D), radius = 6f, center = Offset(w * 0.25f, h * 0.31f))
                drawCircle(Color(0xFFFCD34D), radius = 6f, center = Offset(w * 0.75f, h * 0.31f))
                drawCircle(Color(0xFFFCD34D), radius = 6f, center = Offset(w * 0.5f, h * 0.65f))
                
                drawCircle(Color(0xFFFCD34D), radius = 4f, center = Offset(w * 0.15f, h * 0.75f))
                drawCircle(Color(0xFFFCD34D), radius = 4f, center = Offset(w * 0.85f, h * 0.75f))
            }
            "wanita_selendang" -> {
                // Selendang sash tie
                val pathS = Path().apply {
                    moveTo(w * 0.1f, h * 0.45f)
                    lineTo(w * 0.9f, h * 0.35f)
                    lineTo(w * 0.9f, h * 0.55f)
                    lineTo(w * 0.1f, h * 0.65f)
                    close()
                }
                drawPath(pathS, Color(0xFFE11D48))
                drawPath(pathS, Color.White, style = Stroke(width = 1.5f))
            }
            else -> {
                // Beautiful flower jepun / accessories
                drawCircle(Color(0xFFFDE68A), radius = w * 0.35f, center = center)
                drawCircle(Color.White, radius = w * 0.22f, center = center)
                drawCircle(Color(0xFFF59E0B), radius = 8f, center = center)
            }
        }
    }
}

// ----------------- SHOPPING CART VIEW -----------------

@Composable
fun KeranjangScreen(viewModel: BajuAdatViewModel) {
    val cartItems by viewModel.cartProductItems.collectAsState()
    val deliveryAddr by viewModel.deliveryAddress.collectAsState()
    val deliveryLat by viewModel.deliveryLatitude.collectAsState()
    val deliveryLng by viewModel.deliveryLongitude.collectAsState()
    val aiRouteReport by viewModel.aiRouteAnalysis.collectAsState()
    val aiRouteLoading by viewModel.aiRouteLoading.collectAsState()

    var showMapPicker by remember { mutableStateOf(false) }

    if (showMapPicker) {
        InteractiveMapPicker(
            onLocationSelected = { addr, lat, lng ->
                viewModel.updateShippingLocation(addr, lat, lng)
                showMapPicker = false
            },
            onDismiss = { showMapPicker = false },
            initialLat = deliveryLat,
            initialLng = deliveryLng,
            initialAddress = deliveryAddr
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "Keranjang Belanja",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            if (cartItems.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.RemoveShoppingCart,
                            contentDescription = "Empty",
                            modifier = Modifier.size(64.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Keranjang belanja Anda masih kosong", color = Color.Gray)
                    }
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    // Items List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(cartItems) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cart_item_${item.id}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Visual preview
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ProductImagePlaceholder(imageName = item.product.imageUrl, category = item.product.category)
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            item.product.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            String.format("Rp %,.0f", item.product.price),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // Quantity adjustments
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.updateCartQuantity(item.id, item.quantity - 1) },
                                            modifier = Modifier.size(28.dp).testTag("cart_minus_${item.id}")
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Kurang")
                                        }

                                        Text(
                                            "${item.quantity}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )

                                        IconButton(
                                            onClick = { viewModel.updateCartQuantity(item.id, item.quantity + 1) },
                                            modifier = Modifier.size(28.dp).testTag("cart_plus_${item.id}")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Tambah")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Maps Address Config Panel
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Alamat Pengiriman G-Maps", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }

                                Button(
                                    onClick = { showMapPicker = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.testTag("launch_maps_picker_button")
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pilih Maps", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                deliveryAddr,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                String.format("GPS: %.6f, %.6f", deliveryLat, deliveryLng),
                                fontSize = 10.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            // GEMINI LIGHTNING ROUTE ANALYSIS PREDICTION PREVIEW
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = "AI Route",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "Rute Optimal AI (Gemini Flash-Lite)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (aiRouteLoading) {
                                            LinearProgressIndicator(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                            )
                                        } else {
                                            Text(
                                                if (aiRouteReport.isBlank()) "Silakan pilih titik maps di atas untuk melihat analisis rute kurir AI instan." else aiRouteReport,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Total & Confirm Panel
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .shadow(8.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Belanja", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    String.format("Rp %,.0f", cartItems.sumOf { it.product.price * it.quantity }),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.checkoutCart() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("checkout_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Konfirmasi & Bayar Instan", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------- STATUS TRANS (HISTORY) VIEW -----------------

@Composable
fun StatusScreen(viewModel: BajuAdatViewModel) {
    val transactions by viewModel.transactions.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Riwayat & Status Pesanan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        if (transactions.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = "No trans",
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Belum ada riwayat transaksi", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(transactions) { trans ->
                    var isExpanded by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_card_${trans.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Invoice Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("INV-0082${trans.id}", fontWeight = FontWeight.Black, color = Color.DarkGray)
                                    Text(trans.date, fontSize = 11.sp, color = Color.Gray)
                                }

                                // Status badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when (trans.status) {
                                                "Selesai" -> Color(0xFFD1FAE5)
                                                "Dikirim" -> Color(0xFFDBEAFE)
                                                else -> Color(0xFFFEF3C7)
                                            }
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        trans.status,
                                        color = when (trans.status) {
                                            "Selesai" -> Color(0xFF065F46)
                                            "Dikirim" -> Color(0xFF1E40AF)
                                            else -> Color(0xFF92400E)
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = Color.LightGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Total and Expandable
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("PENGIRIMAN: ${trans.eta}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(
                                        String.format("Rp %,.0f", trans.totalPrice),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                TextButton(onClick = { isExpanded = !isExpanded }) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (isExpanded) "Sembunyikan" else "Rincian Item")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val parsedItems = remember(trans.itemsJson) {
                                        try {
                                            val list = mutableListOf<ParsedItem>()
                                            val array = JSONArray(trans.itemsJson)
                                            for (i in 0 until array.length()) {
                                                val obj = array.getJSONObject(i)
                                                list.add(
                                                    ParsedItem(
                                                        name = obj.getString("name"),
                                                        qty = obj.getInt("qty"),
                                                        price = obj.getDouble("price")
                                                    )
                                                )
                                            }
                                            list
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }

                                    if (parsedItems == null) {
                                        Text("Detail item tidak dapat dimuat", fontSize = 12.sp, color = Color.Red)
                                    } else {
                                        parsedItems.forEach { item ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    "${item.name} x${item.qty}",
                                                    fontSize = 13.sp,
                                                    color = Color.Gray
                                                )
                                                Text(
                                                    String.format("Rp %,.0f", item.price * item.qty),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("ALAMAT TUJUAN", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text(trans.shippingAddress, fontSize = 12.sp)
                                    Text("Koordinat GPS: ${trans.latitude}, ${trans.longitude}", fontSize = 10.sp, color = Color.DarkGray)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // GEMINI ROUTE REPORT SAVED IN TRANSACTION
                            if (trans.aiDispatchReport.isNotBlank()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7).copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFFB45309),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                "Rencana Distribusi Kurir Gemini AI",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFB45309)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                trans.aiDispatchReport,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------- USER PROFILE VIEW -----------------

@Composable
fun ProfilScreen(viewModel: BajuAdatViewModel, onNavigateToBeranda: () -> Unit) {
    val user by viewModel.currentUser.collectAsState()
    val context = LocalContext.current

    // Form inputs state
    var editFullName by remember(user) { mutableStateOf(user?.fullName ?: "") }
    var editPhone by remember(user) { mutableStateOf(user?.phone ?: "") }
    var editAddress by remember(user) { mutableStateOf(user?.address ?: "") }
    var editLat by remember(user) { mutableStateOf(user?.latitude ?: -8.5069) }
    var editLng by remember(user) { mutableStateOf(user?.longitude ?: 115.2625) }

    var showProfileMapPicker by remember { mutableStateOf(false) }
    var showNotifSuccess by remember { mutableStateOf(false) }

    if (showProfileMapPicker) {
        InteractiveMapPicker(
            onLocationSelected = { addr, lat, lng ->
                editAddress = addr
                editLat = lat
                editLng = lng
                showProfileMapPicker = false
            },
            onDismiss = { showProfileMapPicker = false },
            initialLat = editLat,
            initialLng = editLng,
            initialAddress = editAddress
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile circular avatar image holder
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    user?.fullName?.firstOrNull()?.toString()?.uppercase() ?: "U",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                user?.fullName ?: "Nama Pengguna",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                user?.email ?: "email@domain.com",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Profile Edit inputs
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Informasi Profil & Alamat Kurir",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = editFullName,
                        onValueChange = { editFullName = it },
                        label = { Text("Nama Lengkap") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_input_name"),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("No. Whatsapp / Telepon") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_input_phone"),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true
                    )

                    Column {
                        OutlinedTextField(
                            value = editAddress,
                            onValueChange = { editAddress = it },
                            label = { Text("Alamat Rumah Tergeocoding") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_input_address"),
                            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                            maxLines = 3
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { showProfileMapPicker = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_map_trigger")
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Atur Koordinat di G-Maps")
                        }

                        Text(
                            String.format("Koordinat GPS Rumah: %.6f, %.6f", editLat, editLng),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showNotifSuccess) {
                Text(
                    "Profil sukses disimpan di database!",
                    color = Color(0xFF047857),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Save Actions
            Button(
                onClick = {
                    viewModel.updateUserProfile(editFullName, editPhone, editAddress, editLat, editLng)
                    showNotifSuccess = true
                    onNavigateToBeranda()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("profile_save_button"),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simpan Informasi Profil", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    viewModel.logoutUser()
                    try {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                        GoogleSignIn.getClient(context, gso).signOut()
                    } catch (e: Exception) {
                        // ignore
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("profile_logout_button"),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar (Logout)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ----------------- TANYA AI COMPANION DIALOG -----------------

@Composable
fun AiAssistantDialog(
    viewModel: BajuAdatViewModel,
    onDismiss: () -> Unit
) {
    val aiResponse by viewModel.aiTextResponse.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val cartItems by viewModel.cartProductItems.collectAsState()

    var chatInput by remember { mutableStateOf("") }
    val chatHistory = remember {
        mutableStateListOf(
            Pair("assistant", "Om Swastyastu! Saya Bli Gede, asisten personal busana adat Bali Anda. Ada yang bisa saya bantu memadupadankan kamen, kebaya, atau udeng pilihan Anda hari ini? 🌸")
        )
    }

    // Append AI response when generated
    LaunchedEffect(aiResponse) {
        if (aiResponse.isNotBlank()) {
            chatHistory.add(Pair("assistant", aiResponse))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFD97706))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Konsultan AI Bli Gede", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chatHistory) { (sender, text) ->
                            val isAssistant = sender == "assistant"
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = if (isAssistant) Alignment.CenterStart else Alignment.CenterEnd
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isAssistant) {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (isAssistant) 0.dp else 12.dp,
                                        bottomEnd = if (isAssistant) 12.dp else 0.dp
                                    ),
                                    modifier = Modifier.widthIn(max = 220.dp)
                                ) {
                                    Text(
                                        text = text,
                                        modifier = Modifier.padding(10.dp),
                                        fontSize = 12.sp,
                                        color = if (isAssistant) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            Color.White
                                        },
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        if (aiLoading) {
                            item {
                                Row(
                                    modifier = Modifier.padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Bli Gede sedang mengetik rujukan...", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Suggestion chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickQueries = listOf(
                        "Padanan Safari Putih?",
                        "Makna Saput Poleng?",
                        "Tips Kebaya Kuning?",
                        "Pakaian Sembahyang?"
                    )
                    quickQueries.forEach { q ->
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    chatHistory.add(Pair("user", q))
                                    viewModel.askStylingAssistant(q)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(q, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Input bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        placeholder = { Text("Tanyakan busana adat...", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_chat_input"),
                        shape = RoundedCornerShape(20.dp),
                        maxLines = 2,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (chatInput.isNotBlank()) {
                                val query = chatInput
                                chatHistory.add(Pair("user", query))
                                chatInput = ""
                                viewModel.askStylingAssistant(query)
                            }
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .size(36.dp)
                            .testTag("ai_chat_send_button")
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Kirim",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Selesai")
            }
        },
        modifier = Modifier.testTag("ai_assistant_dialog")
    )
}

data class ParsedItem(val name: String, val qty: Int, val price: Double)
