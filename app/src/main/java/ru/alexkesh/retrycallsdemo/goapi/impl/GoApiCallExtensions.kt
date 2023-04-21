package ru.alexkesh.retrycallsdemo.goapi.impl

import ru.alexkesh.retrycallsdemo.goapi.api.GoApiCall
import ru.alexkesh.retrycallsdemo.goapi.api.models.GoApiResponse

suspend fun <T : Any> GoApiCall<T>.singleRequest(): T {
    return singleRequestFull().dto
}

suspend fun <T : Any> GoApiCall<T>.requestFull(
    maxRetries: Int = DEFAULT_MAX_CLIENT_RETRIES,
    onError: ((Throwable) -> Unit)? = null
): GoApiResponse<T> {
    return makeRequestWithRetry(maxRetries = maxRetries, onError = onError)
}

suspend fun <T : Any> GoApiCall<T>.request(
    maxRetries: Int = DEFAULT_MAX_CLIENT_RETRIES,
    onError: ((Throwable) -> Unit)? = null
): T {
    return makeRequestWithRetry(maxRetries = maxRetries, onError = onError).dto
}

suspend inline fun <T : Any> request(callFactory: () -> GoApiCall<T>): T {
    return callFactory().request()
}