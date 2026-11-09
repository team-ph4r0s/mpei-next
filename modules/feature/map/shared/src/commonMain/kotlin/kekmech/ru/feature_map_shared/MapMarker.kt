package kekmech.ru.feature_map_shared

import kotlinx.serialization.Serializable

/** A place on the campus map: a building, a cafe, a hostel, a department. The same record as in markers.json. */
@Serializable
public data class MapMarker(
    val uid: String,
    val name: String,
    val address: String = "",
    val location: Location,
    /** A value that this version of the app does not know becomes [MarkerType.UNDEFINED]. */
    val type: MarkerType = MarkerType.UNDEFINED,
    val icon: String? = null,
    val tag: String? = null,
)

@Serializable
public data class Location(val lat: Double, val lng: Double)

private const val METERS_PER_DEGREE = 111_195.0
private const val DEGREES_IN_HALF_TURN = 180.0

/**
 * The place that is [eastMeters] to the east and [northMeters] to the north of this one (on the ground; precise enough for
 * the tens of meters that two maps of the same campus can differ by).
 */
public fun Location.offsetByMeters(eastMeters: Double, northMeters: Double): Location {
    val metersPerDegreeOfLongitude = METERS_PER_DEGREE * kotlin.math.cos(lat * kotlin.math.PI / DEGREES_IN_HALF_TURN)
    return Location(
        lat = lat + northMeters / METERS_PER_DEGREE,
        lng = lng + eastMeters / metersPerDegreeOfLongitude,
    )
}

@Serializable
public enum class MarkerType {
    FOOD,
    BUILDING,
    HOSTEL,
    OTHER,
    STRUCTURE,
    UNDEFINED,
}

/**
 * The markers that suit the tab and the search: [type] `null` is all the tabs; the query is looked for in the name, the
 * address and the tag, the case does not matter.
 */
public fun filterMarkers(markers: List<MapMarker>, type: MarkerType? = null, query: String = ""): List<MapMarker> {
    val needle = query.trim()
    return markers.filter { marker ->
        (type == null || marker.type == type) &&
            (
                needle.isEmpty() ||
                    marker.name.contains(needle, ignoreCase = true) ||
                    marker.address.contains(needle, ignoreCase = true) ||
                    marker.tag?.contains(needle, ignoreCase = true) == true
                )
    }
}
