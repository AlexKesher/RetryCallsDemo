package ru.alexkesh.retrycallsdemo.goapi.api.models

class GoApiResponse<T : Any>(
    val dto: T,
    val code: Int,
    val headers: Headers
)

class Headers(private val rawHeaders: Map<String, List<String>>) {
    fun header(key: String): String? = rawHeaders[key]?.firstOrNull()

    fun headers(key: String): List<String>? = rawHeaders[key]

    override fun toString(): String {
        return rawHeaders.toString()
    }
}