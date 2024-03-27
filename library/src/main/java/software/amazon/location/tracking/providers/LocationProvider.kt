package software.amazon.location.tracking.providers

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.annotation.RequiresPermission
import software.amazon.location.tracking.config.LocationTrackerConfig
import software.amazon.location.tracking.util.Logger
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


/**
 * LocationProvider is a class that provides methods for retrieving the device's last known location,
 * checking location permission, and subscribing to location updates.
 * @param context The application context.
 * @param locationTrackerConfig The optional configuration for the location client.
 * @property fusedLocationClient The fused location provider client.
 * @property coroutineScope The coroutine scope.
 * @property locationTrackerConfig The optional configuration for the location client.
 */
class LocationProvider(
    context: Context,
    private var locationTrackerConfig: LocationTrackerConfig
) {
    private var fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    /**
     * Subscribes to location updates using the provided location callback.
     *
     * The method first checks the availability of the device's location. If the location is available,
     * it requests location updates using the fused location provider client and the provided location callback.
     *
     * @param locationCallback the location callback to be notified of location updates
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun subscribeToLocationUpdates(locationCallback: LocationCallback) {
        coroutineScope.launch {
            val frequency = locationTrackerConfig.frequency
            val accuracy = locationTrackerConfig.accuracy
            val latency = locationTrackerConfig.latency
            val waitForAccurateLocation = locationTrackerConfig.waitForAccurateLocation
            val minUpdateIntervalMillis = locationTrackerConfig.minUpdateIntervalMillis
            fusedLocationClient.locationAvailability.addOnSuccessListener {
                if (!it.isLocationAvailable) {
                    return@addOnSuccessListener
                }

                fusedLocationClient.requestLocationUpdates(
                    LocationRequest.Builder(accuracy, frequency)
                        .setWaitForAccurateLocation(waitForAccurateLocation)
                        .setMinUpdateIntervalMillis(minUpdateIntervalMillis)
                        .setMaxUpdateDelayMillis(latency)
                        .build(),
                    locationCallback,
                    Looper.getMainLooper(),
                )

            }
        }
    }

    /**
     * Unsubscribes from location updates.
     *
     * @param locationCallback The [LocationCallback] used to receive location updates.
     *
     * @throws SecurityException if the calling package does not have the
     * necessary permissions.
     */
    fun unsubscribeFromLocationUpdates(locationCallback: LocationCallback) {
        coroutineScope.launch {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    /**
     * Retrieves the current location of the device from the fused location provider client
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun getDeviceLocation(): Location? {
        val currentLocationRequest: CurrentLocationRequest = CurrentLocationRequest.Builder()
            .setMaxUpdateAgeMillis(1000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()
        val locationResult =
            fusedLocationClient.getCurrentLocation(
                currentLocationRequest,
                CancellationTokenSource().token
            ).await()
        Logger.log("Location received: ${locationResult?.longitude}, ${locationResult?.latitude}")
        return locationResult
    }
}
