package software.amazon.location.tracking.config

import com.google.android.gms.location.Priority
import software.amazon.location.tracking.filters.DistanceLocationFilter
import software.amazon.location.tracking.filters.LocationFilter
import software.amazon.location.tracking.filters.TimeLocationFilter
import software.amazon.location.tracking.util.TrackingSdkLogLevel

/**
 * Configuration for the LocationTracker.
 * @property trackerName the name of the tracker to use
 * @property locationFilters filters to apply for location updates
 * @property logLevel the log level for SDK logging
 * @property accuracy the desired accuracy for location updates
 * @property latency the maximum latency for location updates
 * @property frequency the desired frequency for location updates
 * @property waitForAccurateLocation flag to wait for an accurate location
 * @property minUpdateIntervalMillis the minimum interval between location updates
 */
data class LocationTrackerConfig(
    // Required
    var trackerName: String,

    // Optional
    var locationFilters: MutableList<LocationFilter> = mutableListOf(
        TimeLocationFilter(),
        DistanceLocationFilter(),
    ),
    var logLevel: TrackingSdkLogLevel = TrackingSdkLogLevel.DEBUG,
    var accuracy: Int = Priority.PRIORITY_HIGH_ACCURACY,
    var latency: Long = 1000,
    var frequency: Long = 1500,
    var waitForAccurateLocation: Boolean = false,
    var minUpdateIntervalMillis: Long = 1000,
    var persistentNotificationConfig: NotificationConfig = NotificationConfig(),
)
