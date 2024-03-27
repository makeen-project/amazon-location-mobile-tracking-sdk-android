package software.amazon.location.tracking.filters

import software.amazon.location.tracking.database.LocationEntry

/**
 * Interface defining the contract for location filters.
 * Implementations of this interface determine whether a location should be uploaded based on specific criteria.
 */
interface LocationFilter {
    /**
     * Determines if a location should be uploaded based on the implemented filter criteria.
     * @param currentLocation The current location to evaluate.
     * @param previousLocation The last uploaded location, or null if there is no previous location.
     * @return True if the current location meets the criteria for upload; false otherwise.
     */
    fun shouldUpload(currentLocation: LocationEntry, previousLocation: LocationEntry?): Boolean
}
