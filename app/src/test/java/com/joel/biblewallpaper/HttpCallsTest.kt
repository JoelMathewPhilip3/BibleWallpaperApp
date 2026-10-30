package com.joel.biblewallpaper

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class HttpCallsTest {
    @Test fun successfulResponseCanBeReadAndClosed() = runBlocking {
        val server=MockWebServer()
        val client=OkHttpClient()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("landscape-response"))
            val call=client.newCall(Request.Builder().url(server.url("/photo")).build())
            withTimeout(5000) {
                call.awaitResponse().use { response ->
                    assertEquals(200,response.code)
                    assertEquals("landscape-response",response.body!!.string())
                }
            }
            assertEquals("/photo",server.takeRequest(5,TimeUnit.SECONDS)!!.path)
        } finally { client.dispatcher.executorService.shutdown(); client.connectionPool.evictAll(); server.shutdown() }
    }
    @Test fun stoppingWorkerCancelsAnInFlightDownload() = runBlocking {
        val server=MockWebServer()
        val client=OkHttpClient()
        server.start()
        try {
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val call=client.newCall(Request.Builder().url(server.url("/slow")).build())
            val download=launch(start=CoroutineStart.UNDISPATCHED) { call.awaitResponse().use {} }
            assertNotNull(server.takeRequest(5,TimeUnit.SECONDS))
            withTimeout(5000) { download.cancelAndJoin() }
            assertTrue(call.isCanceled())
            assertTrue(download.isCancelled)
        } finally { client.dispatcher.executorService.shutdown(); client.connectionPool.evictAll(); server.shutdown() }
    }
}
