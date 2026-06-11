package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val email: String,
    val password: String,
    val fullName: String,
    val phone: String,
    val address: String = "Jl. Raya Ubud No. 10, Gianyar, Bali",
    val latitude: Double = -8.5069,
    val longitude: Double = 115.2625
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val price: Double,
    val category: String, // "Pria" or "Wanita"
    val imageUrl: String, // Placeholder or category tag
    val stock: Int = 10
)

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val productId: Int,
    val quantity: Int
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val totalPrice: Double,
    val date: String,
    val status: String, // "Diproses", "Dikirim", "Selesai"
    val itemsJson: String, // JSON representation of items
    val shippingAddress: String,
    val latitude: Double,
    val longitude: Double,
    val eta: String,
    val aiDispatchReport: String = ""
)

@Dao
interface AppDao {
    // Auth Queries
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Int): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    // Product Queries
    @Query("SELECT * FROM products")
    fun getAllProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<Product>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    // Cart Queries
    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    fun getCartFlow(userId: Int): Flow<List<CartItem>>

    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    suspend fun getCartList(userId: Int): List<CartItem>

    @Query("SELECT * FROM cart_items WHERE userId = :userId AND productId = :productId LIMIT 1")
    suspend fun getCartItem(userId: Int, productId: Int): CartItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(cartItem: CartItem)

    @Update
    suspend fun updateCartItem(cartItem: CartItem)

    @Delete
    suspend fun deleteCartItem(cartItem: CartItem)

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clearCart(userId: Int)

    // Transaction Queries
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY id DESC")
    fun getTransactionsFlow(userId: Int): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long
}

@Database(entities = [User::class, Product::class, CartItem::class, Transaction::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bajuadat_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class BajuAdatRepository(private val appDao: AppDao) {
    val allProducts: Flow<List<Product>> = appDao.getAllProductsFlow()

    suspend fun getUserByEmail(email: String): User? = appDao.getUserByEmail(email)
    suspend fun getUserById(id: Int): User? = appDao.getUserById(id)
    suspend fun registerUser(user: User): Long = appDao.insertUser(user)
    suspend fun updateUser(user: User) = appDao.updateUser(user)

    fun getCart(userId: Int): Flow<List<CartItem>> = appDao.getCartFlow(userId)
    suspend fun getCartList(userId: Int): List<CartItem> = appDao.getCartList(userId)
    suspend fun addToCart(userId: Int, productId: Int, quantity: Int) {
        val existing = appDao.getCartItem(userId, productId)
        if (existing != null) {
            appDao.updateCartItem(existing.copy(quantity = existing.quantity + quantity))
        } else {
            appDao.insertCartItem(CartItem(userId = userId, productId = productId, quantity = quantity))
        }
    }
    suspend fun updateCartQuantity(userId: Int, productId: Int, quantity: Int) {
        val existing = appDao.getCartItem(userId, productId)
        if (existing != null) {
            if (quantity <= 0) {
                appDao.deleteCartItem(existing)
            } else {
                appDao.updateCartItem(existing.copy(quantity = quantity))
            }
        }
    }
    suspend fun removeFromCart(userId: Int, productId: Int) {
        val existing = appDao.getCartItem(userId, productId)
        if (existing != null) {
            appDao.deleteCartItem(existing)
        }
    }
    suspend fun clearCart(userId: Int) = appDao.clearCart(userId)

    fun getTransactions(userId: Int): Flow<List<Transaction>> = appDao.getTransactionsFlow(userId)
    suspend fun createTransaction(transaction: Transaction): Long = appDao.insertTransaction(transaction)

    suspend fun getProductById(id: Int): Product? = appDao.getProductById(id)

    suspend fun seedProductsIfEmpty() {
        val existing = appDao.getAllProducts()
        if (existing.isEmpty()) {
            val seed = listOf(
                // Kategori: Pria
                Product(
                    name = "Baju Safari Pria Putih Agung",
                    description = "Kemeja Safari Adat Bali lengan pendek berwarna putih bersih berkualitas tinggi dengan bahan katun premium. Nyaman, menyerap keringat, dan didesain pas untuk upacara formal adat Bali.",
                    price = 185000.0,
                    category = "Pria",
                    imageUrl = "pria_safari"
                ),
                Product(
                    name = "Kamen Songket Bali Pria Premium",
                    description = "Kamar sarung (Kamen) motif songket khas Bali ditenun indah menggunakan benang emas mewah. Menambahkan kesan agung, berwibawa, dan gagah bagi pemakainya.",
                    price = 275000.0,
                    category = "Pria",
                    imageUrl = "pria_kamen_songket"
                ),
                Product(
                    name = "Udeng Dewata Putih Polos",
                    description = "Ikat kepala tradisional khas Bali (Udeng) warna putih bersih dengan lipatan simetris yang melambangkan kejernihan pikiran dalam beribadah.",
                    price = 45000.0,
                    category = "Pria",
                    imageUrl = "pria_udeng"
                ),
                Product(
                    name = "Saput Poleng Bali Klasik",
                    description = "Kain pelapis luar (Saput) dengan corak kotak-kotak hitam putih (Poleng) khas Bali yang melambangkan keseimbangan harmoni alam semesta (Rua Bhineda).",
                    price = 95000.0,
                    category = "Pria",
                    imageUrl = "pria_saput"
                ),
                // Kategori: Wanita
                Product(
                    name = "Kebaya Bali Brokat Kuning Kunyit",
                    description = "Kebaya wanita Bali bahan brokat premium lembut berwarna kuning kunyit yang cantik dan memesona. Dilengkapi dengan detail renda presisi dan bahan lentur mengikuti bentuk tubuh.",
                    price = 195000.0,
                    category = "Wanita",
                    imageUrl = "wanita_kebaya"
                ),
                Product(
                    name = "Kamen Prada Wanita Merah Mas",
                    description = "Kamen wanita motif prada khas Bali dengan warna dasar merah marun dihiasi sablon gilap emas elegan. Kain katun tebal namun dingin saat dipakai.",
                    price = 165000.0,
                    category = "Wanita",
                    imageUrl = "wanita_kamen_prada"
                ),
                Product(
                    name = "Selendang Bali (Anteng) Cerah",
                    description = "Selendang ikat pinggang sutra tipis mengkilap berhias renda indah di kedua ujungnya. Melambangkan pengendalian emosi dalam tradisi adat wanita Bali.",
                    price = 35000.0,
                    category = "Wanita",
                    imageUrl = "wanita_selendang"
                ),
                Product(
                    name = "Sanggul Bali Set & Jepun",
                    description = "Set sanggul Bali tradisional instan dengan hiasan kelopak bunga jepun (Kamboja) tiruan cantik, memberi kesan alami pada riasan kepala wanita.",
                    price = 85000.0,
                    category = "Wanita",
                    imageUrl = "wanita_sanggul"
                )
            )
            appDao.insertProducts(seed)
        }
    }
}
