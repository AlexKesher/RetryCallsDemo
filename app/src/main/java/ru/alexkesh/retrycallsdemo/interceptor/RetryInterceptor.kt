package ru.alexkesh.retrycallsdemo.interceptor

import java.io.IOException
import kotlin.math.pow
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.internal.http.RealInterceptorChain
import ru.alexkesh.retrycallsdemo.goapi.api.models.RetryAction
import ru.alexkesh.retrycallsdemo.goapi.impl.HEADER_RETRY_LAST_HTTP_STATUS_CODE
import ru.alexkesh.retrycallsdemo.goapi.impl.HEADER_RETRY_NUMBER
import ru.alexkesh.retrycallsdemo.goapi.impl.HEADER_TAXI_RETRY_ACTION
import ru.alexkesh.retrycallsdemo.goapi.impl.HEADER_TAXI_RETRY_INTERVAL_MS
import ru.alexkesh.retrycallsdemo.goapi.impl.STOP_ACTION
import ru.alexkesh.retrycallsdemo.goapi.impl.isRetryableError

class RetryInterceptor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var retryNumber = 0
        var lastStatusCode = 0
        val originalRequest = chain.request()
        var response: Response? = null

        val realChain = chain as RealInterceptorChain
        val call = realChain.call()

        while (true) {
            try {
                if (call.isCanceled()) {
                    response?.close()
                    return Response.Builder()
                        .code(499)
                        .body("Some response body".toResponseBody())
                        .protocol(Protocol.HTTP_2)
                        .message("Some message")
                        .request(originalRequest)
                        .build()
                }

                val newRequestBuilder = originalRequest.newBuilder()
                if (retryNumber > 0) {
                    newRequestBuilder.header(HEADER_RETRY_NUMBER, "$retryNumber")
                }
                if (lastStatusCode != 0) {
                    newRequestBuilder.header(HEADER_RETRY_LAST_HTTP_STATUS_CODE, "$lastStatusCode")
                }

                response?.close()
                response = chain.proceed(newRequestBuilder.build())
                if (response.isSuccessful && response.body != null) {
                    return response
                } else {
                    throw HttpRequestException(response.code, response.headers)
                }
            } catch (e: IOException) {
                if (e is HttpRequestException && isRetryableError(e.code)) {
                    lastStatusCode = e.code
                }

                when (val retryAction = extractRetryAction(retryNumber, e)) {
                    RetryAction.Stop -> throw e
                    is RetryAction.FixedDelay -> sleep(retryAction)
                }
            } finally {
                retryNumber++
            }
        }
    }

    private fun sleep(retryAction: RetryAction.FixedDelay) {
        try {
            Thread.sleep(retryAction.delayMs)
        } catch (e: InterruptedException) {

        }
    }

    private fun extractRetryAction(retryNumber: Int, ex: Exception): RetryAction {
        val result = when {
            ex is HttpRequestException -> when {
                isRetryableError(ex.code) -> safeParseRetryAction(ex.headers)
                else -> RetryAction.Stop
            }
            ex is IOException && ex.message == "Canceled" -> RetryAction.Stop
            else -> null
        }
        return result ?: defaultRetryInterval(attempt = retryNumber)
    }

    private fun defaultRetryInterval(attempt: Int): RetryAction {
        return when (attempt) {
            in 0 until DEFAULT_MAX_CLIENT_RETRIES -> RetryAction.FixedDelay(BASE_SEED * 2.0.pow(attempt).toLong())
            else -> RetryAction.Stop
        }
    }

    private fun safeParseRetryAction(headers: Headers): RetryAction? {
        return try {
            parseRetryAction(headers)
        } catch (ex: Throwable) {
            null
        }
    }

    private fun parseRetryAction(headers: Headers): RetryAction? {
        return when (headers[HEADER_TAXI_RETRY_ACTION]) {
            STOP_ACTION -> RetryAction.Stop
            else -> parseRetryActionInterval(headers)
        }
    }

    private fun parseRetryActionInterval(headers: Headers): RetryAction? {
        val retryInterval = headers[HEADER_TAXI_RETRY_INTERVAL_MS]?.toLongOrNull()
        if (retryInterval != null && retryInterval > 0) {
            return RetryAction.FixedDelay(delayMs = retryInterval)
        }

        return null
    }

    companion object {
        private const val DEFAULT_MAX_CLIENT_RETRIES = 5
        private const val BASE_SEED = 500L
    }
}

class HttpRequestException(
    val code: Int,
    val headers: Headers
) : IOException()