package software.amazon.location.tracking.aws

import software.amazon.location.tracking.database.LocationEntry

/**
 * Callback interface for location tracking events.
 */
interface LocationTrackingCallback {
    /**
     * Called when a new location is received.
     * @param location The new location entry.
     */
    fun onLocationReceived(location: LocationEntry)

    /**
     * Called when the upload of location entries is started.
     * @param entries The list of location entries to be uploaded.
     */
    fun onUploadStarted(entries: List<LocationEntry>)

    /**
     * Called when the location entries have been successfully uploaded.
     * @param entries The list of uploaded location entries.
     */
    fun onUploaded(entries: List<LocationEntry>)

    /**
     * Called when the location upload is skipped due to filters.
     */
    fun onUploadSkipped(entries: LocationEntry)

/**
     * Called when the location availability changes.
     * @param locationAvailable True if the location is available, false otherwise.
     */
    fun onLocationAvailabilityChanged(locationAvailable: Boolean)
}