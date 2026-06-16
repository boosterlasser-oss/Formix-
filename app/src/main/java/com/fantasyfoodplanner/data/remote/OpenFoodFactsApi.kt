package com.fantasyfoodplanner.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ════════════════════════════════════════════
// DTOs
// ════════════════════════════════════════════

data class OffResponseDto(
    val status: Int? = null,            // 1 = found, 0 = not found
    val product: OffProductDto? = null
)

data class OffProductDto(
    val code: String? = null,
    @SerializedName("product_name") val productName: String? = null,
    val brands: String? = null,
    val quantity: String? = null,
    @SerializedName("serving_size") val servingSize: String? = null,
    val nutriments: OffNutrimentsDto? = null,
    @SerializedName("nutrition_grades") val nutritionGrades: String? = null,
    @SerializedName("image_front_url") val imageFrontUrl: String? = null
)

data class OffNutrimentsDto(
    @SerializedName("energy-kcal_100g") val energyKcal100g: Double? = null,
    @SerializedName("proteins_100g") val proteins100g: Double? = null,
    @SerializedName("carbohydrates_100g") val carbohydrates100g: Double? = null,
    @SerializedName("fat_100g") val fat100g: Double? = null,
    // Fallback per serving
    @SerializedName("energy-kcal_serving") val energyKcalServing: Double? = null,
    @SerializedName("proteins_serving") val proteinsServing: Double? = null,
    @SerializedName("carbohydrates_serving") val carbohydratesServing: Double? = null,
    @SerializedName("fat_serving") val fatServing: Double? = null
)

// ════════════════════════════════════════════
// RETROFIT SERVICE
// ════════════════════════════════════════════

interface OpenFoodFactsApi {

    @GET("api/v2/product/{code}")
    suspend fun getProduct(
        @Path("code") code: String,
        @Query("fields") fields: String = "code,product_name,brands,quantity,serving_size,nutriments,nutrition_grades,image_front_url"
    ): OffResponseDto
}

// ════════════════════════════════════════════
// CLIENT SINGLETON
// ════════════════════════════════════════════

object OffClient {

    /**
     * User-Agent Header für Open Food Facts.
     * Ändere App-Name/Version/Contact hier.
     */
    private const val USER_AGENT = "Fitnessplaner/3.0.0 (Android; contact: support@fantasyfoodplanner.example)"

    private val userAgentInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", USER_AGENT)
            .build()
        chain.proceed(request)
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    val api: OpenFoodFactsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFoodFactsApi::class.java)
    }
}

