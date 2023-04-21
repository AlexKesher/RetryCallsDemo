package ru.alexkesh.retrycallsdemo.goapi.api.models

sealed interface RetryAction {
    object Stop : RetryAction

    data class FixedDelay(val delayMs: Long) : RetryAction
}