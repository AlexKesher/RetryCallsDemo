package ru.alexkesh.retrycallsdemo

import com.squareup.moshi.Moshi
import java.net.SocketTimeoutException
import junit.framework.TestCase.fail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import ru.alexkesh.retrycallsdemo.goapi.api.models.GoApiHttpException
import ru.alexkesh.retrycallsdemo.goapi.api.models.GoApiOtherException
import ru.alexkesh.retrycallsdemo.goapi.impl.HEADER_RETRY_LAST_HTTP_STATUS_CODE
import ru.alexkesh.retrycallsdemo.goapi.impl.HEADER_RETRY_NUMBER
import ru.alexkesh.retrycallsdemo.goapi.impl.HEADER_TAXI_RETRY_ACTION
import ru.alexkesh.retrycallsdemo.goapi.impl.STOP_ACTION
import ru.alexkesh.retrycallsdemo.goapi.impl.request
import ru.alexkesh.retrycallsdemo.goapi.impl.requestFull
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

@OptIn(ExperimentalCoroutinesApi::class)
class GoApiCallTests {

    private val mockWebServer = MockWebServer()
    private val moshiAdapter = Moshi.Builder().build().adapter(AwesomeDto::class.java)

    private val baseUrl: String
        get() = mockWebServer.url("").toString()

    @Before
    fun setup() {
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `first request is successful`() = runTest {
        val api = createApi(baseUrl)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(AwesomeDto("Alex").toJson())
        )

        val response = api.awesome(AwesomeParam("2")).requestFull()

        expectThat(response.code).isEqualTo(200)
        expectThat(response.dto).isEqualTo(AwesomeDto("Alex"))
    }

    @Test
    fun `retry after two 500 is successful`() = runTest {
        val api = createApi(baseUrl)
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(AwesomeDto("Bob").toJson())
        )

        val response = api.awesome(AwesomeParam("1")).requestFull()

        expectThat(response.code).isEqualTo(200)
        expectThat(response.dto).isEqualTo(AwesomeDto("Bob"))
    }

    @Test(expected = GoApiHttpException::class)
    fun `retry stops by action`() = runTest {
        val api = createApi(baseUrl)
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .addHeader(HEADER_TAXI_RETRY_ACTION, STOP_ACTION)
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(AwesomeDto("Bob").toJson())
        )
        api.awesome(AwesomeParam("1")).request()
    }

    @Test
    fun `retry number header is sent with request after failure`() = runTest {
        val api = createApi(baseUrl)
        val fakeDispatcher = FakeDispatcher(
            listOf(
                { MockResponse().setResponseCode(500) },
                { MockResponse().setResponseCode(500) },
                {
                    MockResponse()
                        .setResponseCode(200)
                        .setBody(AwesomeDto("Bob").toJson())
                }
            )
        )
        mockWebServer.dispatcher = fakeDispatcher

        api.awesome(AwesomeParam("1")).request()

        val lastRequest = fakeDispatcher.lastRequest
        expectThat(lastRequest!!.headers).contains(HEADER_RETRY_NUMBER to "2")
    }

    @Test
    fun `last status code number header is sent to request after failure`() = runTest {
        val api = createApi(baseUrl)
        val fakeDispatcher = FakeDispatcher(
            listOf(
                { MockResponse().setResponseCode(500) },
                { MockResponse().setResponseCode(501) },
                { MockResponse().setResponseCode(502) },
                {
                    MockResponse()
                        .setResponseCode(200)
                        .setBody(AwesomeDto("Bob").toJson())
                }
            )
        )
        mockWebServer.dispatcher = fakeDispatcher

        api.awesome(AwesomeParam("1")).request()

        val lastRequest = fakeDispatcher.lastRequest
        expectThat(lastRequest!!.headers).contains(HEADER_RETRY_LAST_HTTP_STATUS_CODE to "502")
    }

    @Test
    fun `retry after socket timeout`() = runTest {
        val api = createApi(baseUrl)
        val fakeDispatcher = FakeDispatcher(
            listOf(
                { throw SocketTimeoutException() },
                { MockResponse().setResponseCode(500) },
                {
                    MockResponse()
                        .setResponseCode(200)
                        .setBody(AwesomeDto("Bob").toJson())
                }
            )
        )
        mockWebServer.dispatcher = fakeDispatcher

        val request = api.awesome(AwesomeParam("1")).request()

        expectThat(request).isEqualTo(AwesomeDto("Bob"))
    }

    @Test
    fun `socket timeout increases retry number`() = runTest {
        val api = createApi(baseUrl)
        val fakeDispatcher = FakeDispatcher(
            listOf(
                { MockResponse().setResponseCode(500) },
                { throw SocketTimeoutException() },
                {
                    MockResponse()
                        .setResponseCode(200)
                        .setBody(AwesomeDto("Bob").toJson())
                }
            )
        )
        mockWebServer.dispatcher = fakeDispatcher

        api.awesome(AwesomeParam("1")).request()

        val lastRequest = fakeDispatcher.lastRequest
        expectThat(lastRequest!!.headers).contains(HEADER_RETRY_NUMBER to "2")
    }

    @Test
    fun `do not send last status code after socket timeout`() = runTest {
        val api = createApi(baseUrl)
        val fakeDispatcher = FakeDispatcher(
            listOf(
                { throw SocketTimeoutException() },
                {
                    MockResponse()
                        .setResponseCode(200)
                        .setBody(AwesomeDto("Bob").toJson())
                }
            )
        )
        mockWebServer.dispatcher = fakeDispatcher

        api.awesome(AwesomeParam("1")).request()

        val lastRequestHeaders = fakeDispatcher.lastRequest!!.headers
        expectThat(lastRequestHeaders[HEADER_RETRY_LAST_HTTP_STATUS_CODE]).isNull()
    }

    @Test(expected = GoApiOtherException::class)
    fun `retry stops when max attempts reached if has no server response`() = runTest {
        val api = createApi(baseUrl)
        val fakeDispatcher = FakeDispatcher(
            listOf(
                { throw SocketTimeoutException() },
                { throw SocketTimeoutException() },
                { throw SocketTimeoutException() },
                { throw SocketTimeoutException() }
            )
        )
        mockWebServer.dispatcher = fakeDispatcher

        api.awesome(AwesomeParam("1")).request(maxRetries = 3)
    }

    @Test(expected = GoApiHttpException::class)
    fun `do not retry after response with non-retryable 4XX status code`() = runTest {
        val api = createApi(baseUrl)
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(AwesomeDto("Bob").toJson())
        )

        api.awesome(AwesomeParam("1")).request()
    }

    @Test(expected = GoApiHttpException::class)
    fun `do not retry after response with 504 status code`() = runTest {
        val api = createApi(baseUrl)
        mockWebServer.enqueue(MockResponse().setResponseCode(504))
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(AwesomeDto("Bob").toJson())
        )

        api.awesome(AwesomeParam("1")).request()
    }

    private fun AwesomeDto.toJson(): String {
        return moshiAdapter.toJson(this)
    }
}

private class FakeDispatcher(private val responses: List<() -> MockResponse>) : Dispatcher() {
    private var requestIndex = -1

    var lastRequest: RecordedRequest? = null
        private set

    override fun dispatch(request: RecordedRequest): MockResponse {
        requestIndex++
        lastRequest = request
        if (requestIndex !in responses.indices) {
            fail("Have no fake response for request number: ${requestIndex + 1}, request: $request")
        }

        return responses[requestIndex].invoke()
    }
}