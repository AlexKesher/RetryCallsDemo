package ru.alexkesh.retrycallsdemo.goapi.api

import ru.alexkesh.retrycallsdemo.goapi.api.models.GoApiResponse

interface GoApiCall<T : Any> {
    /**
     * @throws GoApiException when request finished with exception
     * */
    suspend fun singleRequestFull(): GoApiResponse<T>
}