package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class CartProductItem(
    val id: Int,
    val product: Product,
    val quantity: Int
)

class BajuAdatViewModel(application: Application) : AndroidViewModel(application) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val functions by lazy { FirebaseFunctions.getInstance() }

    // UI Session State
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Auth Actions Status
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    fun setAuthError(message: String?) {
        _authError.value = message
    }

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    // Products State
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    // Cart List combined with full Product details
    private val _cartProductItems = MutableStateFlow<List<CartProductItem>>(emptyList())
    val cartProductItems: StateFlow<List<CartProductItem>> = _cartProductItems.asStateFlow()

    // Transactions History Flow
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    // Active coordinates for shipping delivery from Google Maps Selectors
    val deliveryLatitude = MutableStateFlow(-8.5069)
    val deliveryLongitude = MutableStateFlow(115.2625)
    val deliveryAddress = MutableStateFlow("Jl. Raya Ubud No. 12, Ubud, Gianyar, Bali")

    // AI Assistant (Bli Gede) Chat State
    private val _aiTextResponse = MutableStateFlow("")
    val aiTextResponse: StateFlow<String> = _aiTextResponse.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    // AI Maps Routing optimization analysis from Gemini Flash-lite
    private val _aiRouteAnalysis = MutableStateFlow("")
    val aiRouteAnalysis: StateFlow<String> = _aiRouteAnalysis.asStateFlow()

    private val _aiRouteLoading = MutableStateFlow(false)
    val aiRouteLoading: StateFlow<Boolean> = _aiRouteLoading.asStateFlow()

    private var productsRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var cartsRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var ordersRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        try {
            com.google.firebase.FirebaseApp.initializeApp(application)
        } catch (e: Exception) {
            // Already initialized or in testing
        }

        // 1. Observe Firebase Auth state and dynamically sync real-time user profiles/carts/orders
        viewModelScope.launch {
            val fUser = auth.currentUser
            if (fUser != null) {
                loadUserProfile(fUser.uid)
            }
        }

        // 2. Keep shipping address vectors updated based on user's current locations
        viewModelScope.launch {
            currentUser.collectLatest { user ->
                if (user != null) {
                    deliveryLatitude.value = user.latitude
                    deliveryLongitude.value = user.longitude
                    deliveryAddress.value = user.address
                }
            }
        }
    }

    fun startProductsListener() {
        if (productsRegistration != null) return
        productsRegistration = firestore.collection("produk")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val pIdStr = doc.getString("productId") ?: doc.id
                        val idVal = try { pIdStr.toInt() } catch (e: Exception) { doc.id.hashCode() }
                        val priceVal = doc.getDouble("harga") ?: (doc.getLong("harga")?.toDouble() ?: 0.0)
                        
                        val isWanita = doc.getString("gambarUrl")?.startsWith("wanita") == true
                        val catVal = if (isWanita) "Wanita" else "Pria"
                        Product(
                            id = idVal,
                            name = doc.getString("nama") ?: "",
                            description = doc.getString("deskripsi") ?: "",
                            price = priceVal,
                            category = catVal,
                            imageUrl = doc.getString("gambarUrl") ?: "",
                            stock = 10
                        )
                    }
                    _products.value = list
                }
            }
    }

    fun seedDatabaseIfEmpty() {
        viewModelScope.launch {
            try {
                firestore.collection("kategori").get().addOnSuccessListener { categoriesSnapshot ->
                    if (categoriesSnapshot.isEmpty) {
                        val categoriesSeed = listOf(
                            Kategori("kat_kamen", "Kain Kamen"),
                            Kategori("kat_kebaya", "Baju Kebaya Adat"),
                            Kategori("kat_udeng", "Udeng"),
                            Kategori("kat_lengkap", "Pakaian Lengkap")
                        )
                        val batch = firestore.batch()
                        categoriesSeed.forEach { item ->
                            val docRef = firestore.collection("kategori").document(item.kategoriId)
                            batch.set(docRef, item)
                        }
                        batch.commit()
                    }
                }

                firestore.collection("produk").get().addOnSuccessListener { productsSnapshot ->
                    if (productsSnapshot.isEmpty) {
                        val csvSeed = listOf(
                            Produk(
                                productId = "1",
                                nama = "Baju Safari Pria Putih Agung",
                                harga = 185000,
                                deskripsi = "Kemeja Safari Adat Bali lengan pendek berwarna putih bersih berkualitas tinggi dengan bahan katun premium. Nyaman, menyerap keringat, dan didesain pas untuk upacara formal adat Bali.",
                                kategoriId = "kat_lengkap",
                                gambarUrl = "pria_safari"
                            ),
                            Produk(
                                productId = "2",
                                nama = "Kamen Songket Bali Pria Premium",
                                harga = 275000,
                                deskripsi = "Kamar sarung (Kamen) motif songket khas Bali ditenun indah menggunakan benang emas mewah. Menambahkan kesan agung, berwibawa, dan gagah bagi pemakainya.",
                                kategoriId = "kat_kamen",
                                gambarUrl = "pria_kamen_songket"
                            ),
                            Produk(
                                productId = "3",
                                nama = "Udeng Dewata Putih Polos",
                                harga = 45000,
                                deskripsi = "Ikat kepala tradisional khas Bali (Udeng) warna putih bersih dengan lipatan simetris yang melambangkan kejernihan pikiran dalam beribadah.",
                                kategoriId = "kat_udeng",
                                gambarUrl = "pria_udeng"
                            ),
                            Produk(
                                productId = "4",
                                nama = "Saput Poleng Bali Klasik",
                                harga = 95000,
                                deskripsi = "Kain pelapis luar (Saput) dengan corak kotak-kotak hitam putih (Poleng) khas Bali yang melambangkan keseimbangan harmoni alam semesta (Rua Bhineda).",
                                kategoriId = "kat_kamen",
                                gambarUrl = "pria_saput"
                            ),
                            Produk(
                                productId = "5",
                                nama = "Kebaya Bali Brokat Kuning Kunyit",
                                harga = 195000,
                                deskripsi = "Kebaya wanita Bali bahan brokat premium lembut berwarna kuning kunyit yang cantik dan memesona. Dilengkapi dengan detail renda presisi dan bahan lentur mengikuti bentuk tubuh.",
                                kategoriId = "kat_kebaya",
                                gambarUrl = "wanita_kebaya"
                            ),
                            Produk(
                                productId = "6",
                                nama = "Kamen Prada Wanita Merah Mas",
                                harga = 165000,
                                deskripsi = "Kamen wanita motif prada khas Bali dengan warna dasar merah marun dihiasi sablon gilap emas elegan. Kain katun tebal namun dingin saat dipakai.",
                                kategoriId = "kat_kamen",
                                gambarUrl = "wanita_kamen_prada"
                            )
                        )
                        val batch = firestore.batch()
                        csvSeed.forEach { item ->
                            val docRef = firestore.collection("produk").document(item.productId)
                            batch.set(docRef, item)
                        }
                        batch.commit()
                    }
                }
            } catch (e: Exception) {
                // Ignore or log
            }
        }
    }

    private fun loadUserProfile(uid: String) {
        firestore.collection("users").document(uid)
            .addSnapshotListener { doc, error ->
                if (error != null) return@addSnapshotListener
                if (doc != null && doc.exists()) {
                    val emailVal = doc.getString("email") ?: ""
                    val fullNameVal = doc.getString("fullName") ?: ""
                    val phoneVal = doc.getString("phone") ?: ""
                    val addressVal = doc.getString("address") ?: "Jl. Raya Ubud No. 10, Gianyar, Bali"
                    val latVal = doc.getDouble("latitude") ?: -8.5069
                    val lngVal = doc.getDouble("longitude") ?: 115.2625
                    
                    val user = User(
                        id = 1,
                        email = emailVal,
                        password = "",
                        fullName = fullNameVal,
                        phone = phoneVal,
                        address = addressVal,
                        latitude = latVal,
                        longitude = lngVal
                    )
                    _currentUser.value = user
                    
                    // Start listener collections
                    startProductsListener()
                    startCartsListener(uid)
                    startOrdersListener(uid)
                }
            }
    }

    private fun startCartsListener(uid: String) {
        cartsRegistration?.remove()
        cartsRegistration = firestore.collection("cart")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        val pIdStr = doc.getString("productId") ?: return@mapNotNull null
                        val pId = pIdStr.toIntOrNull() ?: pIdStr.hashCode()
                        val qty = doc.getLong("kuantitas")?.toInt() ?: 1
                        val product = _products.value.firstOrNull { it.id == pId } ?: Product(
                            id = pId,
                            name = doc.getString("namaProduk") ?: "",
                            description = "",
                            price = (doc.getLong("harga") ?: 0L).toDouble(),
                            category = if (doc.getString("gambarUrl")?.startsWith("wanita") == true) "Wanita" else "Pria",
                            imageUrl = doc.getString("gambarUrl") ?: ""
                        )
                        
                        CartProductItem(
                            id = pId,
                            product = product,
                            quantity = qty
                        )
                    }
                    _cartProductItems.value = items
                }
            }
    }

    private fun startOrdersListener(uid: String) {
        ordersRegistration?.remove()
        ordersRegistration = firestore.collection("orders")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val daftarProdukList = doc.get("daftarProduk") as? List<Map<String, Any>> ?: emptyList()
                        val array = JSONArray()
                        daftarProdukList.forEach { itemMap ->
                            val obj = JSONObject()
                            obj.put("name", itemMap["namaProduk"])
                            obj.put("qty", itemMap["kuantitas"])
                            obj.put("price", itemMap["harga"])
                            array.put(obj)
                        }
                        
                        val orderIdString = doc.getString("orderId") ?: doc.id
                        val timestamp = doc.getTimestamp("tanggalTransaksi")
                        val dateString = if (timestamp != null) {
                            val format = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                            format.format(timestamp.toDate())
                        } else {
                            doc.getString("date") ?: ""
                        }

                        Transaction(
                            id = orderIdString.hashCode(),
                            userId = 1,
                            totalPrice = doc.getDouble("totalHarga") ?: (doc.getLong("totalHarga")?.toDouble() ?: 0.0),
                            date = dateString,
                            status = doc.getString("statusPesanan") ?: "Diproses",
                            itemsJson = array.toString(),
                            shippingAddress = doc.getString("alamatPengiriman") ?: "",
                            latitude = doc.getDouble("latitude") ?: -8.5069,
                            longitude = doc.getDouble("longitude") ?: 115.2625,
                            eta = doc.getString("eta") ?: "1-2 Hari Kerja",
                            aiDispatchReport = doc.getString("aiDispatchReport") ?: ""
                        )
                    }.sortedByDescending { it.date }
                    _transactions.value = list
                }
            }
    }

    // ---------------- AUTHENTICATION ----------------

    fun registerUser(email: String, sandi: String, namaLength: String, telp: String) {
        viewModelScope.launch {
            _authError.value = null
            if (email.isBlank() || sandi.isBlank() || namaLength.isBlank() || telp.isBlank()) {
                _authError.value = "Semua bidang registrasi harus diisi!"
                return@launch
            }
            _isAuthLoading.value = true
            try {
                auth.createUserWithEmailAndPassword(email, sandi)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = task.result?.user?.uid ?: ""
                            val userProfile = hashMapOf(
                                "email" to email,
                                "fullName" to namaLength,
                                "phone" to telp,
                                "address" to "Jl. Raya Ubud No. 10, Gianyar, Bali",
                                "latitude" to -8.5069,
                                "longitude" to 115.2625
                            )
                            firestore.collection("users").document(uid).set(userProfile)
                                .addOnSuccessListener {
                                    loadUserProfile(uid)
                                    _isAuthLoading.value = false
                                }
                                .addOnFailureListener { e ->
                                    _isAuthLoading.value = false
                                    _authError.value = "Gagal membuat profil: ${e.localizedMessage}"
                                }
                        } else {
                            _isAuthLoading.value = false
                            _authError.value = "Gagal mendaftar: ${task.exception?.localizedMessage}"
                        }
                    }
                    .addOnFailureListener { e ->
                        _isAuthLoading.value = false
                        _authError.value = e.localizedMessage
                    }
            } catch (e: Exception) {
                _isAuthLoading.value = false
                _authError.value = "Gagal mendaftar: ${e.localizedMessage}"
            }
        }
    }

    fun loginUser(email: String, sandi: String) {
        viewModelScope.launch {
            _authError.value = null
            if (email.isBlank() || sandi.isBlank()) {
                _authError.value = "Email dan sandi tidak boleh kosong!"
                return@launch
            }
            _isAuthLoading.value = true
            try {
                auth.signInWithEmailAndPassword(email, sandi)
                    .addOnCompleteListener { task ->
                        _isAuthLoading.value = false
                        if (task.isSuccessful) {
                            val uid = task.result?.user?.uid ?: ""
                            loadUserProfile(uid)
                        } else {
                            _authError.value = "Email atau sandi salah!"
                        }
                    }
                    .addOnFailureListener { e ->
                        _isAuthLoading.value = false
                        _authError.value = e.localizedMessage
                    }
            } catch (e: Exception) {
                _isAuthLoading.value = false
                _authError.value = "Gagal login: ${e.localizedMessage}"
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authError.value = null
            _isAuthLoading.value = true
            try {
                val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = task.result?.user?.uid ?: ""
                            firestore.collection("users").document(uid).get()
                                .addOnSuccessListener { doc ->
                                    if (!doc.exists()) {
                                        val userProfile = hashMapOf(
                                            "email" to (task.result?.user?.email ?: ""),
                                            "fullName" to (task.result?.user?.displayName ?: "Pengguna Google"),
                                            "phone" to "",
                                            "address" to "Jl. Raya Ubud No. 10, Gianyar, Bali",
                                            "latitude" to -8.5069,
                                            "longitude" to 115.2625
                                        )
                                        firestore.collection("users").document(uid).set(userProfile)
                                            .addOnSuccessListener {
                                                loadUserProfile(uid)
                                                _isAuthLoading.value = false
                                            }
                                            .addOnFailureListener { e ->
                                                _isAuthLoading.value = false
                                                _authError.value = "Gagal membuat profil Google: ${e.localizedMessage}"
                                            }
                                    } else {
                                        loadUserProfile(uid)
                                        _isAuthLoading.value = false
                                    }
                                }
                                .addOnFailureListener { e ->
                                    _isAuthLoading.value = false
                                    _authError.value = "Gagal memproses profil Google: ${e.localizedMessage}"
                                }
                        } else {
                            _isAuthLoading.value = false
                            _authError.value = task.exception?.localizedMessage ?: "Gagal masuk dengan Google"
                        }
                    }
            } catch (e: Exception) {
                _isAuthLoading.value = false
                _authError.value = e.localizedMessage ?: "Terjadi kesalahan Google Auth"
            }
        }
    }

    fun logoutUser() {
        auth.signOut()
        productsRegistration?.remove()
        productsRegistration = null
        cartsRegistration?.remove()
        cartsRegistration = null
        ordersRegistration?.remove()
        ordersRegistration = null
        _currentUser.value = null
        _authError.value = null
        _cartProductItems.value = emptyList()
        _transactions.value = emptyList()
    }

    fun updateUserProfile(fullName: String, phone: String, address: String, lat: Double, lng: Double) {
        val uid = auth.uid ?: return
        viewModelScope.launch {
            val userUpdates = hashMapOf<String, Any>(
                "fullName" to fullName,
                "phone" to phone,
                "address" to address,
                "latitude" to lat,
                "longitude" to lng
            )
            firestore.collection("users").document(uid).update(userUpdates)
        }
    }

    // ---------------- CART & TRANSACTION MANAGEMENT ----------------

    fun addToCart(product: Product, quantity: Int = 1) {
        val uid = auth.uid ?: return
        viewModelScope.launch {
            val docId = "${uid}_${product.id}"
            val docRef = firestore.collection("cart").document(docId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    val currentQty = snapshot.getLong("kuantitas") ?: 0L
                    transaction.update(docRef, "kuantitas", currentQty + quantity)
                } else {
                    val data = hashMapOf(
                        "cartId" to docId,
                        "userId" to uid,
                        "productId" to product.id.toString(),
                        "namaProduk" to product.name,
                        "harga" to product.price.toInt(),
                        "kuantitas" to quantity,
                        "gambarUrl" to product.imageUrl
                    )
                    transaction.set(docRef, data)
                }
            }
        }
    }

    fun updateCartQuantity(cartProdId: Int, quantity: Int) {
        val uid = auth.uid ?: return
        viewModelScope.launch {
            val docId = "${uid}_${cartProdId}"
            val docRef = firestore.collection("cart").document(docId)
            if (quantity <= 0) {
                docRef.delete()
            } else {
                docRef.update("kuantitas", quantity)
            }
        }
    }

    fun removeFromCart(productId: Int) {
        val uid = auth.uid ?: return
        viewModelScope.launch {
            firestore.collection("cart").document("${uid}_${productId}").delete()
        }
    }

    fun clearCart() {
        val uid = auth.uid ?: return
        viewModelScope.launch {
            firestore.collection("cart")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener { query ->
                    firestore.runBatch { batch ->
                        for (doc in query.documents) {
                            batch.delete(doc.reference)
                        }
                    }
                }
        }
    }

    // Interactive address location updates from Maps Selector
    fun updateShippingLocation(alamat: String, lat: Double, lng: Double) {
        deliveryAddress.value = alamat
        deliveryLatitude.value = lat
        deliveryLongitude.value = lng

        // Trigger Gemini route optimization calculation instantly on address confirm
        calculateAiRoutingPrediction(lat, lng, alamat)
    }

    fun checkoutCart() {
        val uid = auth.uid ?: return
        val currentCart = _cartProductItems.value
        if (currentCart.isEmpty()) return

        viewModelScope.launch {
            val total = currentCart.sumOf { it.product.price * it.quantity }.toInt()
            
            val daftarProdukList = currentCart.map {
                hashMapOf(
                    "cartId" to "${uid}_${it.product.id}",
                    "userId" to uid,
                    "productId" to it.product.id.toString(),
                    "namaProduk" to it.product.name,
                    "harga" to it.product.price.toInt(),
                    "kuantitas" to it.quantity,
                    "gambarUrl" to it.product.imageUrl
                )
            }

            val format = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val dateStr = format.format(Date())

            val report = if (_aiRouteAnalysis.value.isNotBlank()) {
                _aiRouteAnalysis.value
            } else {
                "Analisis Rute Instan Gemini Flash-Lite: Memilih kurir terdekat dari HUB Ubud tujuan ${deliveryAddress.value}. Estimasi pengiriman hemat energi diyakini tiba dalam 1-2 hari."
            }

            val orderRef = firestore.collection("orders").document()
            val orderId = orderRef.id

            val orderData = hashMapOf(
                "orderId" to orderId,
                "userId" to uid,
                "daftarProduk" to daftarProdukList,
                "totalHarga" to total,
                "statusPesanan" to "Diproses",
                "shippingAddress" to deliveryAddress.value,
                "latitude" to deliveryLatitude.value,
                "longitude" to deliveryLongitude.value,
                "eta" to "1-2 Hari Kerja",
                "aiDispatchReport" to report,
                "tanggalTransaksi" to com.google.firebase.Timestamp.now()
            )

            orderRef.set(orderData)
                .addOnSuccessListener {
                    clearCart()
                    _aiRouteAnalysis.value = ""
                }
        }
    }

    // ---------------- GEMINI POWERED INTELLIGENCE ----------------

    fun calculateAiRoutingPrediction(lat: Double, lng: Double, address: String) {
        viewModelScope.launch {
            _aiRouteLoading.value = true
            try {
                val report = GeminiAssistant.getRoutingAnalysis(lat, lng, address)
                _aiRouteAnalysis.value = report
            } catch (e: Exception) {
                _aiRouteAnalysis.value = "Rute optimal terpilih: Gianyar Hub -> Seminyak. Estimasi waktu tempuh 45 menit via Go-Send Adat Bali."
            } finally {
                _aiRouteLoading.value = false
            }
        }
    }

    fun askStylingAssistant(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _aiLoading.value = true
            _aiTextResponse.value = ""
            try {
                val cartContext = if (_cartProductItems.value.isEmpty()) {
                    "Keranjang kosong"
                } else {
                    _cartProductItems.value.joinToString { "${it.product.name} (qty: ${it.quantity})" }
                }

                val data = hashMapOf(
                    "query" to query,
                    "cartContext" to cartContext
                )

                functions.getHttpsCallable("askGeminiAssistant")
                    .call(data)
                    .addOnSuccessListener { result ->
                        val resData = result.data as? Map<String, Any>
                        val answer = resData?.get("response") as? String
                            ?: resData?.get("text") as? String
                            ?: "No response from Cloud Function"
                        _aiTextResponse.value = answer
                        _aiLoading.value = false
                    }
                    .addOnFailureListener { e ->
                        viewModelScope.launch {
                            try {
                                val response = GeminiAssistant.getStylingAdvice(query, cartContext)
                                _aiTextResponse.value = response
                            } catch (fallbackEx: Exception) {
                                _aiTextResponse.value = "Suksma atas pertanyaannya Bli! Rekomendasi terbaik disarankan memadukan kamen warna merah mas dengan udeng putih agar tampak agung."
                            } finally {
                                _aiLoading.value = false
                            }
                        }
                    }
            } catch (e: Exception) {
                _aiTextResponse.value = "Gagal menghubungi asisten AI."
                _aiLoading.value = false
            }
        }
    }
}
