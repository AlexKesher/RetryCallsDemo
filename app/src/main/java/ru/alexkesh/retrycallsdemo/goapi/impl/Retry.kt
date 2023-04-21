package ru.alexkesh.retrycallsdemo.goapi.impl

import kotlin.math.pow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import ru.alexkesh.retrycallsdemo.goapi.api.GoApiCall
import ru.alexkesh.retrycallsdemo.goapi.api.models.GoApiException
import ru.alexkesh.retrycallsdemo.goapi.api.models.GoApiHttpException
import ru.alexkesh.retrycallsdemo.goapi.api.models.GoApiOtherException
import ru.alexkesh.retrycallsdemo.goapi.api.models.GoApiResponse
import ru.alexkesh.retrycallsdemo.goapi.api.models.RetryAction

const val BASE_SEED = 500L
const val DEFAULT_MAX_CLIENT_RETRIES = 5

const val SC_TOO_MANY_REQUESTS = 429
const val SC_GATEWAY_TIMEOUT_ERROR = 504

const val HEADER_RETRY_NUMBER = "Retry-Number"
const val HEADER_RETRY_LAST_HTTP_STATUS_CODE = "Retry-Last-Http-Status-Code"
const val HEADER_TAXI_RETRY_ACTION = "Retry-Action"
const val HEADER_TAXI_RETRY_INTERVAL_MS = "Retry-Interval-Ms"

@Suppress("NOTHING_TO_INLINE")
internal suspend inline fun <T : Any> GoApiCall<T>.makeRequestWithRetry(
    maxRetries: Int = DEFAULT_MAX_CLIENT_RETRIES,
    noinline onError: ((Throwable) -> Unit)? = null
): GoApiResponse<T> {
    var retryNumber = 0
    var lastStatusCode = 0

    while (true) {
        currentCoroutineContext().ensureActive()
        try {
            return withContext(requestModifier(retryNumber, lastStatusCode)) {
                singleRequestFull()
            }
        } catch (ex: GoApiException) {
            onError?.invoke(ex)

            if (ex is GoApiHttpException) {
                lastStatusCode = ex.code
            }

            when (val retryAction = extractRetryAction(retryNumber = retryNumber++, ex = ex, maxRetries = maxRetries)) {
                RetryAction.Stop -> throw ex
                is RetryAction.FixedDelay -> delay(retryAction.delayMs)
            }
        }
    }
}

private fun requestModifier(retryNumber: Int, lastStatusCode: Int): RequestModifier {
    return RequestModifier(
        additionalHeaders = buildMap {
            if (retryNumber > 0) {
                put(HEADER_RETRY_NUMBER, "$retryNumber")
            }
            if (lastStatusCode != 0) {
                put(HEADER_RETRY_LAST_HTTP_STATUS_CODE, "$lastStatusCode")
            }
        }
    )
}

private fun extractRetryAction(retryNumber: Int, ex: GoApiException, maxRetries: Int): RetryAction {
    val result = when (ex) {
        is GoApiHttpException -> when {
            isRetryableError(ex.code) -> safeParseRetryAction(ex.headers)
            else -> RetryAction.Stop
        }
        is GoApiOtherException -> null
    }
    return result ?: defaultRetryInterval(attempt = retryNumber, maxClientRetries = maxRetries)
}

private fun defaultRetryInterval(attempt: Int, maxClientRetries: Int): RetryAction {
    return when (attempt) {
        in 0 until maxClientRetries -> RetryAction.FixedDelay(BASE_SEED * 2.0.pow(attempt).toLong())
        else -> RetryAction.Stop
    }
}

fun isRetryableError(code: Int): Boolean {
    return code == SC_TOO_MANY_REQUESTS || (code >= 500 && code != SC_GATEWAY_TIMEOUT_ERROR)
}