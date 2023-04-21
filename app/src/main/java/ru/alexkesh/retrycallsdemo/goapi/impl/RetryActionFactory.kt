package ru.alexkesh.retrycallsdemo.goapi.impl

import ru.alexkesh.retrycallsdemo.goapi.api.models.Headers
import ru.alexkesh.retrycallsdemo.goapi.api.models.RetryAction

const val STOP_ACTION = "stop"

fun safeParseRetryAction(headers: Headers): RetryAction? {
    return try {
        parseRetryAction(headers)
    } catch (ex: Throwable) {
        null
    }
}

fun parseRetryAction(headers: Headers): RetryAction? {
    return when (headers.header(HEADER_TAXI_RETRY_ACTION)) {
        STOP_ACTION -> RetryAction.Stop
        else -> parseRetryActionInterval(headers)
    }
}

private fun parseRetryActionInterval(headers: Headers): RetryAction? {
    val retryInterval = headers.header(HEADER_TAXI_RETRY_INTERVAL_MS)?.toLongOrNull()
    if (retryInterval != null && retryInterval > 0) {
        return RetryAction.FixedDelay(delayMs = retryInterval)
    }

    return null
}