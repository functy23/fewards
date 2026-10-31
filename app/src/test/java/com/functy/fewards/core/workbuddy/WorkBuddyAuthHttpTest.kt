package com.functy.fewards.core.workbuddy

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress

/**
 * 钉刷新请求的「线上形状」：路径、X-Refresh-Token、Authorization、body 必须是 {}，
 * 以及响应里的新 RT 会被带回来（轮换后的 RT 不写回就废链）。
 * 用本地 HttpServer，不打真实上游。
 */
class WorkBuddyAuthHttpTest {

    private fun withServer(
        response: String,
        status: Int = 200,
        block: (WorkBuddyAuth, Capture) -> Unit,
    ) {
        val capture = Capture()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v2/plugin/auth/token/refresh") { exchange ->
            capture.path = exchange.requestURI.path
            capture.refreshToken = exchange.requestHeaders.getFirst("X-Refresh-Token")
            capture.refreshSource = exchange.requestHeaders.getFirst("X-Auth-Refresh-Source")
            capture.authorization = exchange.requestHeaders.getFirst("Authorization")
            capture.userId = exchange.requestHeaders.getFirst("X-User-Id")
            capture.body = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
            val bytes = response.toByteArray()
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val auth = WorkBuddyAuth(OkHttpClient(), "http://127.0.0.1:" + server.address.port)
            block(auth, capture)
        } finally {
            server.stop(0)
        }
    }

    private class Capture {
        var path = ""
        var refreshToken: String? = null
        var refreshSource: String? = null
        var authorization: String? = null
        var userId: String? = null
        var body = ""
    }

    @Test
    fun sendsRotatingTokenAndReturnsNewPair() {
        val payload =
            """{"code":0,"msg":"ok","data":{"accessToken":"at-new","refreshToken":"rt-new","expiresIn":3600}}"""
        withServer(payload) { auth, capture ->
            val result = runBlocking { auth.refresh("at-old", "rt-old", "u-1") }
            assertTrue(result is WorkBuddyAuth.Result.Success)
            val tokens = (result as WorkBuddyAuth.Result.Success).tokens
            assertEquals("at-new", tokens.accessToken)
            assertEquals("rt-new", tokens.refreshToken)
            assertTrue(tokens.expiresAt > 0)

            assertEquals("/v2/plugin/auth/token/refresh", capture.path)
            assertEquals("rt-old", capture.refreshToken)
            assertEquals("plugin", capture.refreshSource)
            assertEquals("Bearer at-old", capture.authorization)
            assertEquals("u-1", capture.userId)
            // 官方是 axios.post(url, {})：body 必须是空对象，不是空 body
            assertEquals("{}", capture.body)
        }
    }

    @Test
    fun mapsUpstream12153ToSessionDead() {
        val payload = """{"code":12153,"msg":"12153:refresh token failed"}"""
        withServer(payload, status = 401) { auth, _ ->
            val result = runBlocking { auth.refresh("at-old", "rt-dead", "u-1") }
            assertEquals(WorkBuddyAuth.Result.SessionDead, result)
        }
    }

    @Test
    fun emptyRefreshTokenNeverHitsTheNetwork() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.start()
        try {
            val auth = WorkBuddyAuth(OkHttpClient(), "http://127.0.0.1:" + server.address.port)
            val result = runBlocking { auth.refresh("at-old", "   ", "u-1") }
            assertTrue(result is WorkBuddyAuth.Result.Failed)
        } finally {
            server.stop(0)
        }
    }
}
