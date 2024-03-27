package software.amazon.location.tracking.filters

import android.location.Location
import software.amazon.location.tracking.config.SdkConfig
import software.amazon.location.tracking.database.LocationEntry

/**
 * Filters location updates based on a specified distance threshold.
 * The filter determines if the distance between the current location and the previous location exceeds the threshold.
 * If it does, the current location is considered significant enough to be uploaded.
 * @param distanceThreshold The minimum distance in meters that needs to be exceeded for the location to be uploaded.
 */
class DistanceLocationFilter(var distanceThreshold: Double = SdkConfig.DEFAULT_DISTANCE_THRESHOLD) :
  LocationFilter {

    /**
     * Determines if the current location should be uploaded.
     * @param currentLocation The current location to be checked.
     * @param previousLocation The previously uploaded location.
     * @return True if the distance between current and previous location is greater than the threshold; false otherwise.
     */
    override fun shouldUpload(
      currentLocation: LocationEntry,
      previousLocation: LocationEntry?,
    ): Boolean {
        previousLocation ?: return true

        val distance = calculateDistance(currentLocation, previousLocation)
        return distance > distanceThreshold
    }

    /**
     * Calculates the distance between two locations.
     * @param currentLocation Current location.
     * @param previousLocation Previous location.
     * @return The distance in meters.
     */
    private fun calculateDistance(currentLocation: LocationEntry, previousLocation: LocationEntry): Double {
        val currentLocationData = Location("LocationProvider").apply {
            latitude = currentLocation.latitude
            longitude = currentLocation.longitude
        }

        val previousLocationData = Location("LocationProvider").apply {
            latitude = previousLocation.latitude
            longitude = previousLocation.longitude
        }

        return currentLocationData.distanceTo(previousLocationData).toDouble()
    }
}
