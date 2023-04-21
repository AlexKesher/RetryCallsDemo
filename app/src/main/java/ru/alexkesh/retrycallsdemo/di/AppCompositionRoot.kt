package ru.alexkesh.retrycallsdemo.di

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.alexkesh.retrycalls.BuildConfig
import ru.alexkesh.retrycallsdemo.endpoints.JsonPlaceholderApi
import ru.alexkesh.retrycallsdemo.goapi.impl.GoApiCallAdapterFactory

private const val BASE_URL = "https://jsonplaceholder.typicode.com"

class AppCompositionRoot {

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            val logger = HttpLoggingInterceptor().setLevel(Level.BODY)
            builder.addInterceptor(logger)
        }
        builder.build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .addCallAdapterFactory(GoApiCallAdapterFactory())
            .build()
    }

    val jsonPlaceholderApi: JsonPlaceholderApi by lazy {
        retrofit.create(JsonPlaceholderApi::class.java)
    }
}