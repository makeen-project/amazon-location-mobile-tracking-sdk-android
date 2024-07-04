package software.amazon.location.tracking

import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.room.RoomDatabase
import aws.sdk.kotlin.services.location.LocationClient
import aws.sdk.kotlin.services.location.model.BatchUpdateDevicePositionResponse
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.GsonBuilder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import software.amazon.location.auth.EncryptedSharedPreferences
import software.amazon.location.auth.LocationCredentialsProvider
import software.amazon.location.tracking.TestConstants.LAST_LOCATION
import software.amazon.location.tracking.TestConstants.TEST_LATITUDE
import software.amazon.location.tracking.TestConstants.TEST_LONGITUDE
import software.amazon.location.tracking.TestConstants.TRACKER_NAME
import software.amazon.location.tracking.aws.AmazonTrackingHttpClient
import software.amazon.location.tracking.aws.LocationTrackingCallback
import software.amazon.location.tracking.config.LocationTrackerConfig
import software.amazon.location.tracking.filters.AccuracyLocationFilter
import software.amazon.location.tracking.filters.DistanceLocationFilter
import software.amazon.location.tracking.filters.TimeLocationFilter
import software.amazon.location.tracking.providers.LocationProvider
import software.amazon.location.tracking.util.Helper
import software.amazon.location.tracking.util.ServiceCallback
import software.amazon.location.tracking.util.StoreKey
import software.amazon.location.tracking.util.TrackingSdkLogLevel

class LocationTrackerWithGetDeviceLocationTest {

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
    }

    @Test
    fun `get Device Location`() {
        every { anyConstructed<Helper>().isGooglePlayServicesAvailable(any()) } returns false
        mockkConstructor(LocationTracker::class)
        coEvery {
            anyConstructed<LocationTracker>().uploadLocationUpdates(any<LocationTrackingCallback>())
        } just runs
        val location = mock(Location::class.java)
        `when`(location.latitude).thenReturn(TEST_LATITUDE)
        `when`(location.longitude).thenReturn(TEST_LONGITUDE)
        val mockAmazonLocationClient = mockk<LocationClient>()
        coEvery { locationCredentialsProvider.isCredentialsValid() } returns true
        coEvery { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } returns location
        coEvery { locationCredentialsProvider.verifyAndRefreshCredentials() } just runs
        coEvery {
            locationCredentialsProvider.getLocationClient()
        } returns mockAmazonLocationClient
        val result = BatchUpdateDevicePositionResponse.invoke {
            errors = listOf()
        }
        coEvery {
            mockAmazonLocationClient.batchUpdateDevicePosition(any())
        } returns result
        val locationTrackingCallback = mockk<LocationTrackingCallback>(relaxed = true)
        val awsKeyValueStore = mockk<EncryptedSharedPreferences>()

        every { awsKeyValueStore.get(any()) } returns "true"
        val locationClient =
            LocationTracker(context, locationCredentialsProvider, locationClientConfig)
        locationClient.enableFilter(TimeLocationFilter())
        locationClient.enableFilter(DistanceLocationFilter())
        locationClient.enableFilter(AccuracyLocationFilter())
        every {
            anyConstructed<EncryptedSharedPreferences>().get(
                StoreKey.IS_ACCURACY_FILTER_ENABLE,
            )
        } returns true.toString()
        every { anyConstructed<EncryptedSharedPreferences>().get(StoreKey.LAST_LOCATION) } returns LAST_LOCATION
        every { anyConstructed<Location>().distanceTo(any()) } returns 20F
        runBlocking {
            val locationResult = locationClient.getDeviceLocation(locationTrackingCallback)
            assertNotNull(locationResult)
        }
    }
}
