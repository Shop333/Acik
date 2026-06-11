package com.example.data

import com.google.firebase.Timestamp

data class Kategori(
    val kategoriId: String = "",
    val nama: String = ""
)

data class Produk(
    val productId: String = "",
    val nama: String = "",
    val harga: Int = 0,
    val deskripsi: String = "",
    val kategoriId: String = "",
    val gambarUrl: String = ""
)

data class Cart(
    val cartId: String = "",
    val userId: String = "",
    val productId: String = "",
    val namaProduk: String = "",
    val harga: Int = 0,
    val kuantitas: Int = 0,
    val gambarUrl: String = ""
)

data class Order(
    val orderId: String = "",
    val userId: String = "",
    val daftarProduk: List<Cart> = emptyList(),
    val totalHarga: Int = 0,
    val statusPesanan: String = "Pending",
    val alamatPengiriman: String = "",
    val tanggalTransaksi: Timestamp? = null
)
