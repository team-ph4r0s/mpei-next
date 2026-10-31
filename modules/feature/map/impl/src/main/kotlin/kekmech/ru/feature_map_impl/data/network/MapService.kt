package kekmech.ru.feature_map_impl.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kekmech.ru.feature_map_impl.data.model.MapMarkerDto
import kekmech.ru.lib_network.BackendServiceUrl
import kekmech.ru.lib_network.ServiceUrlResolver
import kotlinx.serialization.json.Json

internal class MapService(
    private val client: HttpClient,
    private val json: Json,
) {

    suspend fun getMapMarkers(): List<MapMarkerDto> {
        // GitHub raw answers with text/plain, so the body is decoded by hand
        val text = client.get("${ServiceUrlResolver.resolve(BackendServiceUrl.MAP)}markers.json").bodyAsText()
        return json.decodeFromString(text)
    }
}
