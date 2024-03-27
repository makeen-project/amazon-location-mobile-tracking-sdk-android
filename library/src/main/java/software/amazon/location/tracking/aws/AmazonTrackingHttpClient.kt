package software.amazon.location.tracking.aws

import android.content.Context
import android.location.Location
import com.amazonaws.services.geo.AmazonLocationClient
import com.amazonaws.services.geo.model.BatchUpdateDevicePositionRequest
import com.amazonaws.services.geo.model.BatchUpdateDevicePositionResult
import com.amazonaws.services.geo.model.DevicePositionUpdate
import com.amazonaws.services.geo.model.GetDevicePositionRequest
import com.amazonaws.services.geo.model.GetDevicePositionResult
import software.amazon.location.tracking.config.SdkConfig.MAX_RETRY
import software.amazon.location.tracking.providers.DeviceIdProvider
import software.amazon.location.tracking.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Handles interactions with Amazon Location tracking.
 * @param context the application context
 * @param trackerName the name of the tracker
 */
class AmazonTrackingHttpClient(context: Context, private val trackerName: String) {
    private var deviceIdProvider: DeviceIdProvider = DeviceIdProvider(context)

    /**
     * Updates a single device location.
     * @param amazonLocationClient the client used to interact with Amazon Location Service
     * @param location the location to update
     * @return BatchUpdateDevicePositionResult containing the result of the update operation
     */
    suspend fun updateTrackerDeviceLocation(
        amazonLocationClient: AmazonLocationClient,
        location: Location,
    ): BatchUpdateDevicePositionResult? {
        return updateTrackerDeviceLocation(
            amazonLocationClient,
            arrayOf(location),
        )
    }

    /**
     * Updates multiple device locations.
     * @param amazonLocationClient the client used to interact with Amazon Location Service
     * @param locations the locations to update
     * @return BatchUpdateDevicePositionResult containing the result of the update operation
     */
    suspend fun updateTrackerDeviceLocation(
        amazonLocationClient: AmazonLocationClient,
        locations: Array<Location>,
    ): BatchUpdateDevicePositionResult? {
        var retries = 0
        val deviceID = deviceIdProvider.getDeviceID()
        val updates = locations.map { location ->
            DevicePositionUpdate()
                .withSampleTime(Date(location.time))
                .withDeviceId(deviceID)
                .withPosition(location.longitude, location.latitude)
        }
        val batchUpdateRequest = BatchUpdateDevicePositionRequest()
            .withTrackerName(trackerName)
            .withUpdates(updates)

        while (retries < MAX_RETRY) {
            try {
                return withContext(Dispatchers.Default) {
                    amazonLocationClient.batchUpdateDevicePosition(batchUpdateRequest)
                }
            } catch (e: Exception) {
                Logger.log("Update failed. Retrying... (${retries + 1}/$MAX_RETRY)", e)
                retries++
                if (retries == MAX_RETRY) {
                    Logger.log("Update failed. Max retries reached.")
                    throw e
                }
            }
        }
        return null
    }

    /**
     * Retrieves the current device location.
     * @param amazonLocationClient the client used to interact with Amazon Location Service
     * @return GetDevicePositionResult containing the result of the retrieval operation
     */
    suspend fun getTrackerDeviceLocation(
        amazonLocationClient: AmazonLocationClient,
    ): GetDevicePositionResult {
        val deviceID = deviceIdProvider.getDeviceID()
        return withContext(Dispatchers.IO) {
            amazonLocationClient.getDevicePosition(
                GetDevicePositionRequest()
                    .withDeviceId(deviceID)
                    .withTrackerName(trackerName),
            )
        }
    }
}
