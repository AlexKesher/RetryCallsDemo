package ru.alexkesh.retrycallsdemo.goapi.api.models

sealed class GoApiException : RuntimeException()

class GoApiHttpException(
    val code: Int,
    val headers: Headers
) : GoApiException()

class GoApiOtherException(val original: Throwable) : GoApiException()