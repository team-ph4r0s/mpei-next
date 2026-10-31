package kekmech.ru.lib_http

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** What the servers learn about the app: the name of the client, the system, the language and the version. There is no id of the device. */
public class ClientIdentity(
    public val userAgent: String,
    public val deviceOs: String,
    public val deviceLocale: String,
    public val appVersion: String,
)

public val DefaultJson: Json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

private const val REQUEST_TIMEOUT_MILLIS = 15_000L

private fun HttpClientConfig<*>.installTimeouts(timeoutMillis: Long) {
    install(HttpTimeout) {
        requestTimeoutMillis = timeoutMillis
        connectTimeoutMillis = timeoutMillis
        socketTimeoutMillis = timeoutMillis
    }
}

private fun HttpClientConfig<*>.installLogging(logger: ((String) -> Unit)?, logLevel: LogLevel) {
    if (logger == null) return
    install(Logging) {
        this.logger = object : Logger {
            override fun log(message: String) = logger(message)
        }
        level = logLevel
    }
}

/**
 * The HTTP client of the app. Any status other than 2xx is an exception (ClientRequestException,
 * ServerResponseException), and a response is decoded as JSON with [DefaultJson].
 *
 * @param engine the engine; if it is `null`, the engine of the platform is used (OkHttp, Darwin).
 * @param logger gets the log of the requests; the log is off when it is `null`.
 */
public fun createHttpClient(
    identity: ClientIdentity,
    engine: HttpClientEngine? = null,
    logger: ((String) -> Unit)? = null,
    logLevel: LogLevel = LogLevel.INFO,
): HttpClient {
    val config: HttpClientConfig<*>.() -> Unit = {
        expectSuccess = true
        install(ContentNegotiation) { json(DefaultJson) }
        installTimeouts(REQUEST_TIMEOUT_MILLIS)
        defaultRequest {
            header(HttpHeaders.UserAgent, identity.userAgent)
            header("X-Device-Os", identity.deviceOs)
            header("X-Device-Locale", identity.deviceLocale)
            header("X-App-Version", identity.appVersion)
        }
        installLogging(logger, logLevel)
    }
    return if (engine == null) HttpClient(config) else HttpClient(engine, config)
}

/**
 * The HTTP client for the pages of a site that is not ours (the BARS portal): the same engine, timeouts and log as
 * [createHttpClient], but it does not tell the site anything about the app (only [userAgent]), does not ask for JSON
 * and does not turn a status into an exception: the caller reads the pages and the redirects itself.
 *
 * @param engine the engine; if it is `null`, the engine of the platform is used (OkHttp, Darwin).
 * @param configure the rest of the setup: the cookies, the redirects.
 */
public fun createPageHttpClient(
    userAgent: String,
    engine: HttpClientEngine? = null,
    timeoutMillis: Long = REQUEST_TIMEOUT_MILLIS,
    logger: ((String) -> Unit)? = null,
    logLevel: LogLevel = LogLevel.INFO,
    configure: HttpClientConfig<*>.() -> Unit = {},
): HttpClient {
    val config: HttpClientConfig<*>.() -> Unit = {
        expectSuccess = false
        install(UserAgent) { agent = userAgent }
        installTimeouts(timeoutMillis)
        installLogging(logger, logLevel)
        configure()
    }
    return if (engine == null) HttpClient(config) else HttpClient(engine, config)
}
