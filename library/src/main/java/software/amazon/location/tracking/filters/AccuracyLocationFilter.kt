package software.amazon.location.tracking.filters

import android.location.Location
import software.amazon.location.tracking.database.LocationEntry

/**
 * Filters location updates by comparing the distance moved since the last update with the current location's accuracy.
 * If the device has moved a distance greater than the current location's accuracy threshold, the location is considered for upload.
 */
class AccuracyLocationFilter : LocationFilter {

    /**
     * Determines if the current location should be uploaded.
     * @param currentLocation The current location to be checked.
     * @param previousLocation The previously uploaded location.
     * @return True if the current location meets the accuracy and movement criteria; false otherwise.
     */
    override fun shouldUpload(
      currentLocation: LocationEntry,
      previousLocation: LocationEntry?,
    ): Boolean {
        if (previousLocation == null) {
            return true
        }
        val distanceMoved = calculateDistance(currentLocation, previousLocation)
        return distanceMoved > currentLocation.accuracy
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
