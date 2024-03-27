package software.amazon.location.tracking.filters

import software.amazon.location.tracking.config.SdkConfig
import software.amazon.location.tracking.database.LocationEntry

/**
 * Filter that determines if the current location should be uploaded based on a defined time interval.
 * @property timeInterval The time interval in milliseconds between location uploads.
 */
class TimeLocationFilter(var timeInterval: Long = SdkConfig.DEFAULT_TIME_INTERVAL) :
  LocationFilter {

    /**
     * Checks if the current location should be uploaded based on the elapsed time since the last upload.
     * @param currentLocation The location to evaluate.
     * @param previousLocation The last uploaded location, or null if there is no previous location.
     * @return True if the time since the last upload exceeds the specified interval; false otherwise.
     */
    override fun shouldUpload(currentLocation: LocationEntry, previousLocation: LocationEntry?): Boolean {
        if (previousLocation == null) {
            return true
        }

        return currentLocation.time - previousLocation.time > timeInterval
    }
}
