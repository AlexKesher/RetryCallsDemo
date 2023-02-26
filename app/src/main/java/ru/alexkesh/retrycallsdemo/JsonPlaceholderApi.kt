package ru.alexkesh.retrycallsdemo

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface JsonPlaceholderApi {

    @GET("posts/{id}")
    suspend fun getPost(@Path("id") id: Long): Response<Post>
}

@JsonClass(generateAdapter = true)
data class Post(
    val id: Long,
    val title: String,
    val body: String,
    val userId: Long
)
