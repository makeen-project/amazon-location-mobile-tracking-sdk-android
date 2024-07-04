package software.amazon.location.tracking

import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.room.RoomDatabase
import aws.sdk.kotlin.services.location.LocationClient
import com.google.android.gms.location.FusedLocationProviderClient
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
import kotlin.concurrent.thread
import org.junit.Before
import org.junit.Test
import software.amazon.location.auth.EncryptedSharedPreferences
import software.amazon.location.auth.LocationCredentialsProvider
import software.amazon.location.tracking.TestConstants.TRACKER_NAME
import software.amazon.location.tracking.aws.AmazonTrackingHttpClient
import software.amazon.location.tracking.aws.LocationTrackingCallback
import software.amazon.location.tracking.config.LocationTrackerConfig
import software.amazon.location.tracking.providers.LocationProvider
import software.amazon.location.tracking.util.Helper
import software.amazon.location.tracking.util.ServiceCallback
import software.amazon.location.tracking.util.StoreKey
import software.amazon.location.tracking.util.TrackingSdkLogLevel

class LocationTrackerWithTrackerNameTest {


    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

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
        fusedLocationProviderClient = mockk()
        mockkStatic(LocationServices::class)
        mockkStatic(Build.VERSION::class)
        every { LocationServices.getFusedLocationProviderClient(context) } returns fusedLocationProviderClient
        every { anyConstructed<EncryptedSharedPreferences>().get(any()) } returns TRACKER_NAME
        mockkStatic(Log::class)

        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        mockkStatic(Looper::class)
        val mainLooper = mockk<Looper>()
        every { Looper.getMainLooper() } returns mainLooper

        val mainThread = thread { }
        every { mainLooper.thread } returns mainThread
    }

    @Test
    fun `test constructor with tracker name`() {
        val locationClient = LocationTracker(context, locationCredentialsProvider, TRACKER_NAME)

        assertNotNull(locationClient)
    }
}
