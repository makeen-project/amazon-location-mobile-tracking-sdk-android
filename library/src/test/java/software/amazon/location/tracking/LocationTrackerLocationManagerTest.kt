package software.amazon.location.tracking

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.room.RoomDatabase
import aws.sdk.kotlin.services.location.LocationClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.gson.GsonBuilder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import software.amazon.location.auth.EncryptedSharedPreferences
import software.amazon.location.auth.LocationCredentialsProvider
import software.amazon.location.tracking.TestConstants.TRACKER_NAME
import software.amazon.location.tracking.aws.AmazonTrackingHttpClient
import software.amazon.location.tracking.aws.LocationTrackingCallback
import software.amazon.location.tracking.config.LocationTrackerConfig
import software.amazon.location.tracking.config.SdkConfig.MIN_DISTANCE
import software.amazon.location.tracking.database.LocationEntry
import software.amazon.location.tracking.database.LocationEntryDao_Impl
import software.amazon.location.tracking.providers.LocationProvider
import software.amazon.location.tracking.util.Helper
import software.amazon.location.tracking.util.ServiceCallback
import software.amazon.location.tracking.util.StoreKey
import software.amazon.location.tracking.util.TrackingSdkLogLevel

class LocationTrackerLocationManagerTest {

    private lateinit var context: Context
    private lateinit var locationManager: LocationManager
    private lateinit var locationCredentialsProvider: LocationCredentialsProvider
    private lateinit var locationTrackingCallback: LocationTrackingCallback
    private lateinit var serviceCallback: ServiceCallback
    private lateinit var locationClientConfig: LocationTrackerConfig
    private lateinit var gsonBuilderMock: GsonBuilder

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        locationManager = mockk()
        serviceCallback = mockk()
        locationTrackingCallback = mockk()
        locationCredentialsProvider = mockk()
        locationClientConfig = LocationTrackerConfig(
            trackerName = TRACKER_NAME,
            logLevel = TrackingSdkLogLevel.DEBUG,
            accuracy = Priority.PRIORITY_HIGH_ACCURACY,
            waitForAccurateLocation = false,
        )
        gsonBuilderMock = mockk()
        every { context.getSystemService(Context.LOCATION_SERVICE) } returns locationManager
        every { gsonBuilderMock.create() } returns mockk()
        every { gsonBuilderMock.registerTypeAdapter(any(), any()) } returns gsonBuilderMock

        val locationClientConfig = mockk<LocationTrackerConfig>()
        every { locationClientConfig.logLevel } returns TrackingSdkLogLevel.DEBUG

        mockkConstructor(Helper::class)
        every { anyConstructed<Helper>().isGooglePlayServicesAvailable(any()) } returns true
        mockkConstructor(EncryptedSharedPreferences::class)
        every { anyConstructed<EncryptedSharedPreferences>().initEncryptedSharedPreferences() } just runs
        every { anyConstructed<EncryptedSharedPreferences>().get(any()) } returns "mockDeviceID"
        every { anyConstructed<EncryptedSharedPreferences>().contains(StoreKey.DEVICE_ID) } returns true
        every { anyConstructed<EncryptedSharedPreferences>().put(any(), any<String>()) } just runs
        mockkConstructor(LocationCredentialsProvider::class)
        every { anyConstructed<LocationCredentialsProvider>().isCredentialsValid() } returns true
        coEvery { anyConstructed<LocationCredentialsProvider>().verifyAndRefreshCredentials() } just runs
        mockkConstructor(AmazonTrackingHttpClient::class)
        mockkConstructor(LocationProvider::class)
        mockkConstructor(RoomDatabase::class)
        mockkConstructor(LocationClient::class)
        mockkStatic(LocationServices::class)

        mockkConstructor(Location::class)

        every { anyConstructed<Location>().latitude = any() } just runs
        every { anyConstructed<Location>().longitude = any() } just runs
        every { anyConstructed<Location>().time = any() } just runs
        every { anyConstructed<Location>().accuracy = any() } just runs
        every { anyConstructed<Location>().hasAccuracy() } returns true

        val location1 = LocationEntry(1, 10.0, 20.0, System.currentTimeMillis(), 5.0f)
        val locations = listOf(location1)
        mockkConstructor(LocationEntryDao_Impl::class)
        coEvery { anyConstructed<LocationEntryDao_Impl>().getAllEntries() } returns locations
        coEvery { anyConstructed<LocationEntryDao_Impl>().insert(any()) } just runs
        coEvery { anyConstructed<LocationEntryDao_Impl>().deleteEntriesByIds(any()) } just runs
        coEvery { anyConstructed<LocationEntryDao_Impl>().deleteEntryById(any()) } just runs
    }

    @Test
    fun `start and stop location background tracking without fused location`() {
        every { anyConstructed<Helper>().isGooglePlayServicesAvailable(any()) } returns false
        val locationClient =
            LocationTracker(context, locationCredentialsProvider, locationClientConfig)
        val task: Task<LocationAvailability> = mockk()
        every { task.addOnSuccessListener(any()) } answers {
            val listener = arg<OnSuccessListener<LocationAvailability>>(0)
            val locationAvailability = mockk<LocationAvailability> {
                every { isLocationAvailable } returns true
            }
            listener.onSuccess(locationAvailability)
            task
        }
        coEvery { locationManager.removeUpdates(any<LocationListener>()) } just runs
        coEvery { locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, any(), MIN_DISTANCE, any(), Looper.getMainLooper()) } just runs
        coEvery { locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, any(), MIN_DISTANCE, any(), Looper.getMainLooper()) } just runs
        locationClient.startBackgroundLocationUpdates()
        locationClient.stopBackgroundLocationUpdates()

        verify {
            anyConstructed<EncryptedSharedPreferences>().put(
                StoreKey.BG_TRACKING_IN_PROGRESS, false.toString()
            )
        }
    }
}
