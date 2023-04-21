package ru.alexkesh.retrycallsdemo.goapi.impl

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

internal data class RequestModifier(
    val additionalHeaders: Map<String, String>
) : AbstractCoroutineContextElement(RequestModifier) {
    companion object Key : CoroutineContext.Key<RequestModifier>

    override fun toString(): String = "RequestModifier($additionalHeaders)"
}