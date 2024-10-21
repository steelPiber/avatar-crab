package piber.avatar_crab.presentation.data

data class MapPolygonData(
    val email: String,
    val title: String,
    val coordinates: String,   // JSON string of coordinates
    val location_data: List<LocationData>  // List of heart rate data points
)