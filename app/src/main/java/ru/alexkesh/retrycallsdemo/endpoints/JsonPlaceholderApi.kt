package ru.alexkesh.retrycallsdemo.endpoints

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import ru.alexkesh.retrycallsdemo.goapi.api.GoApiCall

interface JsonPlaceholderApi {

    @GET("posts/{id}")
    suspend fun getPost(@Path("id") id: Long): Post

    @GET("posts/{id}")
    fun getPostGoApiCall(@Path("id") id: Long): GoApiCall<Post>
}

@JsonClass(generateAdapter = true)
data class Post(
    val id: Long,
    val title: String,
    val body: String,
    val userId: Long
)