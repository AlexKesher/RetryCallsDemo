package ru.alexkesh.retrycallsdemo.goapi.impl

import okhttp3.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import ru.alexkesh.retrycallsdemo.goapi.api.GoApiCall

class GoApiCallAdapter<T : Any>(
    private val callFactory: Call.Factory,
    private val responseType: Type
) : CallAdapter<T, GoApiCall<T>> {
    override fun responseType(): Type = responseType

    override fun adapt(call: retrofit2.Call<T>): GoApiCall<T> {
        return GoApiCallImpl(callFactory, call)
    }
}

class GoApiCallAdapterFactory : CallAdapter.Factory() {
    override fun get(returnType: Type, annotations: Array<out Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        if (retrofit2.Call::class.java == getRawType(returnType)
            && returnType is ParameterizedType
            && getRawType(getParameterUpperBound(0, returnType)) == GoApiCall::class.java
        ) {
            throw IllegalStateException("Invalid GoApiCall usage. Please remove suspend modifier")
        }

        if (getRawType(returnType) != GoApiCall::class.java) {
            return null
        }

        if (returnType !is ParameterizedType) {
            throw IllegalStateException("GoApiCall missing generic type!")
        }

        return GoApiCallAdapter<Any>(retrofit.callFactory(), getParameterUpperBound(0, returnType))
    }
}