package ru.alexkesh.retrycallsdemo

import com.squareup.moshi.JsonClass
import java.util.concurrent.TimeUnit.MILLISECONDS
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import retrofit2.http.Body
import retrofit2.http.POST
import ru.alexkesh.retrycallsdemo.goapi.api.GoApiCall
import ru.alexkesh.retrycallsdemo.goapi.impl.GoApiCallAdapterFactory

fun createApi(baseUrl: String): AwesomeApi {
    val client = OkHttpClient.Builder()
        .callTimeout(500, MILLISECONDS)
        .build()
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(GoApiCallAdapterFactory())
        .build()
        .create<AwesomeApi>()
}

interface AwesomeApi {
    @POST("awesome")
    fun awesome(@Body param: AwesomeParam): GoApiCall<AwesomeDto>
}

@JsonClass(generateAdapter = true)
data class AwesomeParam(
    val value: String
)

@JsonClass(generateAdapter = true)
data class AwesomeDto(
    val value: String
)
