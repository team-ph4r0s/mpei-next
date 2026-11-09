package kekmech.ru.ui_map

import kekmech.ru.feature_map_shared.TileId

/**
 * One layer of tiles: the images that [url] points to, up to the zoom level [maxLevel].
 *
 * The coordinates of the markers are given for the satellite imagery. A map that is drawn from other data (the scheme of
 * OpenStreetMap) puts the same building a few meters aside, and the markers would stand beside their buildings. So a
 * layer can tell how far its features are from the place that the imagery puts them in: [featuresEastMeters] to the
 * east and [featuresNorthMeters] to the north; the map draws such a layer shifted back by that distance.
 */
public class TileLayer(
    public val id: String,
    public val maxLevel: Int,
    public val featuresEastMeters: Double = 0.0,
    public val featuresNorthMeters: Double = 0.0,
    public val url: (TileId) -> String,
)

/**
 * How far the scheme of OpenStreetMap lies from the Esri imagery on the campus of MPEI: the buildings of the scheme are 4.7 m
 * east and 3.8 m south of the markers (`OSM_FEATURES_NORTH_METERS` is negative: the south). It was measured on the tiles of
 * zoom 18 of the campus with `statics/map/measure_offset.py` (the method and the way to repeat it are in that file). With
 * this shift 42 of the 47 markers of the buildings and of the food places stand inside a building of the scheme, without it
 * 34 do.
 */
internal const val OSM_FEATURES_EAST_METERS = 4.7
internal const val OSM_FEATURES_NORTH_METERS = -3.8

/** What the map shows: the street scheme, or the satellite imagery with the names of the places on top. */
public enum class MapAppearance(public val code: String, public val layers: List<TileLayer>, public val attribution: String) {
    SCHEME(
        code = "scheme",
        layers = listOf(
            TileLayer(
                id = "osm",
                maxLevel = 19,
                featuresEastMeters = OSM_FEATURES_EAST_METERS,
                featuresNorthMeters = OSM_FEATURES_NORTH_METERS,
            ) { "https://tile.openstreetmap.org/${it.z}/${it.x}/${it.y}.png" },
        ),
        attribution = "© OpenStreetMap contributors",
    ),
    HYBRID(
        code = "hybrid",
        layers = listOf(
            TileLayer(id = "esri-imagery", maxLevel = 19) { esriTile("World_Imagery", it) },
            TileLayer(id = "esri-places", maxLevel = 19) {
                esriTile("Reference/World_Boundaries_and_Places", it)
            },
        ),
        attribution = "© Esri, Maxar, Earthstar Geographics",
    );

    public companion object {
        /** The appearance that is saved under [code] in the settings; the scheme if there is none. */
        public fun fromCode(code: String?): MapAppearance = entries.firstOrNull { it.code == code } ?: SCHEME
    }
}

private const val ESRI_BASE = "https://server.arcgisonline.com/ArcGIS/rest/services/"

/** The tiles of Esri are addressed as `.../tile/{z}/{y}/{x}`: the row goes before the column. */
private fun esriTile(service: String, id: TileId): String =
    "$ESRI_BASE$service/MapServer/tile/${id.z}/${id.y}/${id.x}"
