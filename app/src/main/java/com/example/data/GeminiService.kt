package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }
}

object GeminiAssistant {
    private const val MODEL_NAME = "gemini-3.1-flash-lite-preview"

    suspend fun getAiResponse(systemPrompt: String, userPrompt: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Koneksi simulasi aktif: BajuAdat AI siap melayani Anda! (Masukkan GEMINI_API_KEY di panel AI Studio untuk respon langsung)."
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = userPrompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        return try {
            val response = RetrofitClient.service.generateContent(MODEL_NAME, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Maaf, AI sedang beristirahat sejenak."
        } catch (e: Exception) {
            "Simulasi Respon (Gagal menghubungi server: ${e.localizedMessage}): BajuAdat AI sangat menganjurkan pakaian ini karena jahitannya yang halus dan kecocokan motif songket mas dengan safari putih!"
        }
    }

    /**
     * Menggunakan model Gemini Flash-Lite untuk menganalisis koordinat lokasi
     * pengiriman dari Maps, memberikan rute kurir tercepat secara instan.
     */
    suspend fun getRoutingAnalysis(lat: Double, lng: Double, address: String): String {
        val systemPrompt = """
            Anda adalah Koordinator Logistik AI untuk Toko BajuAdat Bali.
            Gunakan model Gemini Flash-Lite berkecepatan tinggi untuk menghasilkan laporan pengiriman singkat (maksimal 2-3 kalimat santun bahasa Indonesia).
            Sebutkan analisis jarak atau durasi pengiriman dari Hub Utama kami di Ubud, Kabupaten Gianyar ke kabupaten/daerah koordinat tersebut (lat: $lat, lng: $lng, alamat: $address).
            Estimasi rute pengiriman harus masuk akal untuk Bali (misalnya melalui bypass Ida Bagus Mantra, Jl. Sunset Road, dll), gunakan gaya bahasa ramah khas Bali.
        """.trimIndent()

        val userPrompt = "Analisis rute pengiriman tercepat untuk alamat ini: $address dengan koordinat ($lat, $lng)."

        return getAiResponse(systemPrompt, userPrompt)
    }

    /**
     * Melayani konsultasi adat dan padu padan warna baju adat bali pria & wanita.
     */
    suspend fun getStylingAdvice(userQuery: String, cartContext: String): String {
        val systemPrompt = """
            Anda adalah 'Bli Gede', Konsultan Busana Adat Bali yang ramah, sopan, dan sangat berpengetahuan luas tentang tradisi Bali.
            Bantu pembeli memilih baju adat Bali yang cocok untuk pria / wanita.
            Berikan saran kombinasi warna, aksesoris seperti keris, selendang, jepun bunga, atau lipatan udeng.
            Berbahasalah Indonesia dengan logat Bali ramah yang santun (menggunakan sapaan Bli, Mbok, Om Swastyastu / Suksma).
            Konteks keranjang belanja saat ini: $cartContext.
        """.trimIndent()

        return getAiResponse(systemPrompt, userQuery)
    }
}
