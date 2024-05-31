package software.amazon.location.tracking.aws

import android.content.Context
import android.location.Location
import aws.sdk.kotlin.services.location.model.BatchEvaluateGeofencesRequest
import aws.sdk.kotlin.services.location.model.BatchEvaluateGeofencesResponse
import aws.sdk.kotlin.services.location.model.BatchUpdateDevicePositionRequest
import aws.sdk.kotlin.services.location.model.BatchUpdateDevicePositionResponse
import aws.sdk.kotlin.services.location.model.DevicePositionUpdate
import aws.sdk.kotlin.services.location.model.GetDevicePositionRequest
import aws.sdk.kotlin.services.location.model.GetDevicePositionResponse
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.fromEpochMilliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.location.auth.LocationCredentialsProvider
import software.amazon.location.tracking.config.SdkConfig.MAX_RETRY
import software.amazon.location.tracking.providers.DeviceIdProvider
import software.amazon.location.tracking.util.Logger

/**
 * Handles interactions with Amazon Location tracking services.
 *
 * @param context the application context
 * @param mTrackerName the name of the tracker
 */
class AmazonTrackingHttpClient(context: Context, private val mTrackerName: String) {
    private var deviceIdProvider: DeviceIdProvider = DeviceIdProvider(context)

    /**
     * Updates the single location.
     *
     * This function sends a single device location to Amazon Location Service.
     *
     * @param locationCredentialsProvider provides the client used to interact with Amazon Location Service
     * @param location the location to update
     * @return BatchUpdateDevicePositionResponse containing the result of the update operation
     */
    suspend fun updateTrackerDeviceLocation(
        locationCredentialsProvider: LocationCredentialsProvider?,
        location: Location,
    ): BatchUpdateDevicePositionResponse? {
        return updateTrackerDeviceLocation(
            locationCredentialsProvider,
            arrayOf(location),
        )
    }

    /**
     * Updates multiple device locations.
     *
     * This function sends a batch of device locations to Amazon Location Service. It retries the operation
     * up to MAX_RETRY times in case of failure.
     *
     * @param locationCredentialsProvider provides the client used to interact with Amazon Location Service
     * @param locations an array of locations to update
     * @return BatchUpdateDevicePositionResponse containing the result of the update operation, or null if the update fails
     * @throws Exception if the update fails after the maximum number of retries
     */
    suspend fun updateTrackerDeviceLocation(
        locationCredentialsProvider: LocationCredentialsProvider?,
        locations: Array<Location>,
    ): BatchUpdateDevicePositionResponse? {
        var retries = 0
        val deviceID = deviceIdProvider.getDeviceID()
        val mUpdates = locations.map { location ->
            DevicePositionUpdate {
                this.deviceId = deviceID
                this.sampleTime = Instant.fromEpochMilliseconds(location.time)
                this.position = listOf(location.longitude, location.latitude)
            }
        }
        val batchUpdateRequest = BatchUpdateDevicePositionRequest {
            trackerName = mTrackerName
            updates = mUpdates
        }
        while (retries < MAX_RETRY) {
            try {
                locationCredentialsProvider?.let {
                    return withContext(Dispatchers.Default) {
                        it.getLocationClient()?.batchUpdateDevicePosition(batchUpdateRequest)
                    }
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
     *
     * This function retrieves the last location of a device from Amazon Location Service.
     *
     * @param locationCredentialsProvider provides the client used to interact with Amazon Location Service
     * @return GetDevicePositionResponse containing the result of the retrieval operation
     */
    suspend fun getTrackerDeviceLocation(
        locationCredentialsProvider: LocationCredentialsProvider?,
    ): GetDevicePositionResponse? {
        if (locationCredentialsProvider?.getLocationClient() == null) throw Exception("Failed to get credentials")
        val deviceID = deviceIdProvider.getDeviceID()
        val request = GetDevicePositionRequest {
            this.trackerName = mTrackerName
            this.deviceId = deviceID
        }
        return withContext(Dispatchers.IO) {
            locationCredentialsProvider.getLocationClient()?.getDevicePosition(
                request
            )
        }
    }

    /**
     * Evaluates geofences for a given location.
     *
     * This function evaluates geofences for a given device location using Amazon Location Service.
     *
     * @param locationCredentialsProvider provides the client used to interact with Amazon Location Service
     * @param geofenceCollectionName the name of the geofence collection
     * @param location the location to evaluate
     * @return BatchEvaluateGeofencesResponse containing the result of the evaluation
     */
    suspend fun batchEvaluateGeofences(
        locationCredentialsProvider: LocationCredentialsProvider?,
        geofenceCollectionName: String,
        location: Location
    ): BatchEvaluateGeofencesResponse? {
        if (locationCredentialsProvider?.getLocationClient() == null) throw Exception("Failed to get credentials")
        val deviceID = deviceIdProvider.getDeviceID()
        val devicePosition = DevicePositionUpdate {
            this.deviceId = deviceID
            this.sampleTime = Instant.fromEpochMilliseconds(location.time)
            this.position = listOf(location.longitude, location.latitude)
        }
        val request = BatchEvaluateGeofencesRequest {
            this.collectionName = geofenceCollectionName
            this.devicePositionUpdates = listOf(devicePosition)
        }
        return withContext(Dispatchers.IO) {
            locationCredentialsProvider.getLocationClient()?.batchEvaluateGeofences(
                request
            )
        }
    }
}
