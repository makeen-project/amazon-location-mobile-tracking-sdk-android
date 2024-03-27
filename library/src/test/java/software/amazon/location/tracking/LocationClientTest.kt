package software.amazon.location.tracking

import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.room.RoomDatabase
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.internal.keyvaluestore.AWSKeyValueStore
import com.amazonaws.services.geo.AmazonLocationClient
import com.amazonaws.services.geo.model.BatchUpdateDevicePositionResult
import com.amazonaws.services.geo.model.GetDevicePositionResult
import com.amazonaws.services.geo.model.PositionalAccuracy
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.gson.GsonBuilder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import software.amazon.location.auth.LocationCredentialsProvider
import software.amazon.location.tracking.TestConstants.LAST_LOCATION
import software.amazon.location.tracking.TestConstants.TEST_CLIENT_CONFIG
import software.amazon.location.tracking.TestConstants.TEST_LATITUDE
import software.amazon.location.tracking.TestConstants.TEST_LONGITUDE
import software.amazon.location.tracking.TestConstants.TRACKER_NAME
import software.amazon.location.tracking.aws.AmazonTrackingHttpClient
import software.amazon.location.tracking.aws.LocationTrackingCallback
import software.amazon.location.tracking.config.LocationTrackerConfig
import software.amazon.location.tracking.database.LocationEntry
import software.amazon.location.tracking.database.LocationEntryDao_Impl
import software.amazon.location.tracking.filters.AccuracyLocationFilter
import software.amazon.location.tracking.filters.DistanceLocationFilter
import software.amazon.location.tracking.filters.TimeLocationFilter
import software.amazon.location.tracking.providers.BackgroundLocationService
import software.amazon.location.tracking.providers.BackgroundTrackingWorker
import software.amazon.location.tracking.providers.LocationProvider
import software.amazon.location.tracking.util.BackgroundTrackingMode
import software.amazon.location.tracking.util.ServiceCallback
import software.amazon.location.tracking.util.StoreKey
import software.amazon.location.tracking.util.TrackingSdkLogLevel
import java.util.Date
import kotlin.concurrent.thread
import kotlin.test.assertFalse

class LocationClientTest {


    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var context: Context
    private lateinit var locationCredentialsProvider: LocationCredentialsProvider
    private lateinit var locationTrackingCallback: LocationTrackingCallback
    private lateinit var serviceCallback: ServiceCallback
    private lateinit var locationClientConfig: LocationTrackerConfig
    private lateinit var gsonBuilderMock: GsonBuilder
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        serviceCallback = mockk()
        locationTrackingCallback = mockk()
        locationCredentialsProvider = mockk()
        locationClientConfig = LocationTrackerConfig(
            trackerName = TRACKER_NAME,
            logLevel = TrackingSdkLogLevel.DEBUG,
            accuracy = Priority.PRIORITY_HIGH_ACCURACY,
            latency = 1000,
            frequency = 5000,
            waitForAccurateLocation = false,
            minUpdateIntervalMillis = 5000,
        )
        gsonBuilderMock = mockk()
        every { gsonBuilderMock.create() } returns mockk()
        every { gsonBuilderMock.registerTypeAdapter(any(), any()) } returns gsonBuilderMock

        val locationClientConfig = mockk<LocationTrackerConfig>()
        every { locationClientConfig.logLevel } returns TrackingSdkLogLevel.DEBUG

        mockkConstructor(AWSKeyValueStore::class)
        mockkConstructor(AmazonTrackingHttpClient::class)
        mockkConstructor(LocationProvider::class)
        mockkConstructor(RoomDatabase::class)
        mockkConstructor(AmazonLocationClient::class)
        mockkConstructor(AWSCredentialsProvider::class)
        every { locationCredentialsProvider.getCredentialsProvider() } returns mockk()
        fusedLocationProviderClient = mockk()
        mockkStatic(LocationServices::class)
        mockkStatic(Build.VERSION::class)
        every { LocationServices.getFusedLocationProviderClient(context) } returns fusedLocationProviderClient
        every { anyConstructed<AWSKeyValueStore>().get(any()) } returns TRACKER_NAME
        val location = mock(Location::class.java)
        `when`(location.latitude).thenReturn(TEST_LATITUDE)
        `when`(location.longitude).thenReturn(TEST_LONGITUDE)
        every {
            fusedLocationProviderClient.getCurrentLocation(
                ofType(CurrentLocationRequest::class),
                any()
            )
        } answers {
            Tasks.forResult(location)
        }
        val mockTask: Task<Void?> = mockk()
        coEvery {
            fusedLocationProviderClient.removeLocationUpdates(any<LocationCallback>())
        } returns mockTask
        every { fusedLocationProviderClient.locationAvailability } returns mockk()
        val locationAvailability = mockk<LocationAvailability>()

        every { locationAvailability.isLocationAvailable } returns true

        coEvery { fusedLocationProviderClient.locationAvailability } returns Tasks.forResult(
            locationAvailability
        )
        mockkStatic(Log::class)

        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        mockkStatic(Looper::class)
        val mainLooper = mockk<Looper>()
        every { Looper.getMainLooper() } returns mainLooper

        val mainThread = thread { }
        every { mainLooper.thread } returns mainThread

        val mockBatchUpdateDevicePositionResult = mockk<BatchUpdateDevicePositionResult>()
        coEvery {
            anyConstructed<AmazonLocationClient>().batchUpdateDevicePosition(any())
        } returns mockBatchUpdateDevicePositionResult

        val getDevicePositionResult = mockk<GetDevicePositionResult>()
        coEvery {
            anyConstructed<AmazonLocationClient>().getDevicePosition(any())
        } returns getDevicePositionResult
        coEvery {
            anyConstructed<AmazonLocationClient>().getDevicePosition(any()).position
        } returns listOf(TEST_LONGITUDE, TEST_LATITUDE)
        coEvery {
            anyConstructed<AmazonLocationClient>().getDevicePosition(any()).sampleTime
        } returns Date()
        coEvery {
            anyConstructed<AmazonLocationClient>().getDevicePosition(any()).accuracy
        } returns PositionalAccuracy().withHorizontal(10.0)

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

        every { anyConstructed<Location>().getAccuracy() } returns 10f
        every { anyConstructed<Location>().getTime() } returns System.currentTimeMillis()
        every { anyConstructed<Location>().getLatitude() } returns TEST_LATITUDE
        every { anyConstructed<Location>().getLongitude() } returns TEST_LONGITUDE
    }

    @Test
    fun `test constructor with LocationClientConfig`() {
        val locationClient =
            LocationTracker(context, locationCredentialsProvider, locationClientConfig)

        assertNotNull(locationClient)
    }

    @Test
    fun `test constructor with tracker name`() {
        val locationClient =
            LocationTracker(context, locationCredentialsProvider, TRACKER_NAME)

        assertNotNull(locationClient)
    }

    @Test
    fun `test constructor with LocationCredentialsProvider`() {
        every { anyConstructed<AWSKeyValueStore>().get(StoreKey.CLIENT_CONFIG) } returns TEST_CLIENT_CONFIG
        val locationClient = LocationTracker(context, locationCredentialsProvider)

        assertNotNull(locationClient)
    }

    @Test
    fun `start and stop location tracking`() {
        val locationClient =
            LocationTracker(context, locationCredentialsProvider, locationClientConfig)
        val locationTrackingCallback = mockk<LocationTrackingCallback>(relaxed = true)
        val task: Task<LocationAvailability> = mockk()
        every { task.addOnSuccessListener(any()) } answers {
            val listener = arg<OnSuccessListener<LocationAvailability>>(0)
            val locationAvailability = mockk<LocationAvailability> {
                every { isLocationAvailable } returns true
            }
            listener.onSuccess(locationAvailability)
            task
        }
        every { fusedLocationProviderClient.locationAvailability } returns task
        val location = Location("provider")
        location.latitude = 10.0
        location.longitude = 20.0
        location.time = System.currentTimeMillis()
        location.accuracy = 10.5f
        val locationResult = LocationResult.create(listOf(location))
        every {
            fusedLocationProviderClient.requestLocationUpdates(
                any(),
                ofType(LocationCallback::class),
                ofType(Looper::class)
            )
        } answers {
            val callbackLambda = it.invocation.args[1] as LocationCallback

            callbackLambda.onLocationResult(locationResult)

            mockk<Task<Void>>()
        }


        runBlocking {
            locationClient.start(locationTrackingCallback)
            locationClient.stop()
            assertFalse(locationClient.isTrackingInForeground())
        }
    }


    @Test
    fun `start and stop location tracking on location availability`() {
        val locationClient =
            LocationTracker(context, locationCredentialsProvider, locationClientConfig)
        val locationTrackingCallback = mockk<LocationTrackingCallback>(relaxed = true)
        val task: Task<LocationAvailability> = mockk()
        every { task.addOnSuccessListener(any()) } answers {
            val listener = arg<OnSuccessListener<LocationAvailability>>(0)
            val locationAvailability = mockk<LocationAvailability> {
                every { isLocationAvailable } returns true
            }
            listener.onSuccess(locationAvailability)
            task
        }
        every { fusedLocationProviderClient.locationAvailability } returns task
        val location = Location("provider")
        location.latitude = 10.0
        location.longitude = 20.0
        location.time = System.currentTimeMillis()
        location.accuracy = 10.5f
        val locationResult = mockk<LocationAvailability>()
        every {
            locationResult.isLocationAvailable
        } returns true

        every {
            fusedLocationProviderClient.requestLocationUpdates(
                any(),
                ofType(LocationCallback::class),
                ofType(Looper::class)
            )
        } answers {
            val callbackLambda = it.invocation.args[1] as LocationCallback

            callbackLambda.onLocationAvailability(locationResult)

            mockk<Task<Void>>()
        }


        runBlocking {
            locationClient.start(locationTrackingCallback)
            locationClient.stop()
            assertFalse(locationClient.isTrackingInForeground())
        }
    }

    @Test
    fun `start and stop location background tracking`() {
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
        every { fusedLocationProviderClient.locationAvailability } returns task
        every {
            fusedLocationProviderClient.requestLocationUpdates(
                any(),
                ofType(LocationCallback::class),
                ofType(Looper::class)
            )
        } returns mockk()
        locationClient.startBackgroundLocationUpdates()
        locationClient.stopBackgroundLocationUpdates()

        verify {
            anyConstructed<AWSKeyValueStore>().put(
                StoreKey.BG_TRACKING_IN_PROGRESS,
                false.toString()
            )
        }
    }

    @Test
    fun `get Device Location`() {
        val locationTrackingCallback = mockk<LocationTrackingCallback>(relaxed = true)
        val locationClient =
            LocationTracker(context, locationCredentialsProvider, locationClientConfig)
        runBlocking {
            val location = locationClient.getDeviceLocation(locationTrackingCallback)
            assertNotNull(location)
        }
    }

    @Test
    fun `get Tracker Device Location`() {
        val locationClient =
            LocationTracker(context, locationCredentialsProvider, locationClientConfig)
        runBlocking {
            val location = locationClient.getTrackerDeviceLocation()
            assertNotNull(location)
        }
    }

    @Test
    fun `uploadLocationUpdates uploads locations and handles callbacks`() {
        val locationTrackingCallback = mockk<LocationTrackingCallback>(relaxed = true)
        val awsKeyValueStore = mockk<AWSKeyValueStore>()

        every { awsKeyValueStore.get(any()) } returns "true"
        val locationClient =
            LocationTracker(context, locationCredentialsProvider, locationClientConfig)
        locationClient.enableFilter(TimeLocationFilter())
        locationClient.enableFilter(DistanceLocationFilter())
        locationClient.enableFilter(AccuracyLocationFilter())
        every {
            anyConstructed<AWSKeyValueStore>().get(
                StoreKey.IS_ACCURACY_FILTER_ENABLE,
            )
        } returns true.toString()
        every { anyConstructed<AWSKeyValueStore>().get(StoreKey.LAST_LOCATION) } returns LAST_LOCATION
        every { anyConstructed<Location>().distanceTo(any()) } returns 20F
        runBlocking {
            locationClient.uploadLocationUpdates(locationTrackingCallback)

            verify { locationTrackingCallback.onUploadStarted(match { it.size == 1 }) }
            verify { locationTrackingCallback.onUploaded(match { it.size == 1 }) }
        }
    }

    @Test
    fun `uploadLocationUpdates uploads locations skipped`() {
        val locationTrackingCallback = mockk<LocationTrackingCallback>(relaxed = true)
        val awsKeyValueStore = mockk<AWSKeyValueStore>()

        every { awsKeyValueStore.get(any()) } returns "true"
        val locationClient =
            LocationTracker(context, locationCredentialsProvider, locationClientConfig)
        locationClient.enableFilter(TimeLocationFilter())
        locationClient.enableFilter(DistanceLocationFilter())
        locationClient.enableFilter(AccuracyLocationFilter())
        every {
            anyConstructed<AWSKeyValueStore>().get(
                StoreKey.IS_ACCURACY_FILTER_ENABLE,
            )
        } returns true.toString()
        every { anyConstructed<AWSKeyValueStore>().get(StoreKey.LAST_LOCATION) } returns LAST_LOCATION
        every { anyConstructed<Location>().distanceTo(any()) } returns 5F
        runBlocking {
            locationClient.uploadLocationUpdates(locationTrackingCallback)

            verify { locationTrackingCallback.onUploadSkipped(match { true }) }
        }
    }

    @Test
    fun `enable Filter`() {
        coroutineScope.launch {
            val locationClient =
                LocationTracker(context, locationCredentialsProvider, locationClientConfig)
            locationClient.enableFilter(TimeLocationFilter())
            locationClient.enableFilter(DistanceLocationFilter())
            locationClient.enableFilter(AccuracyLocationFilter())
            verify {
                anyConstructed<AWSKeyValueStore>().put(
                    StoreKey.IS_ACCURACY_FILTER_ENABLE,
                    true.toString()
                )
            }
        }
    }

    @Test
    fun `disable Filter`() {
        coroutineScope.launch {
            val locationClient =
                LocationTracker(context, locationCredentialsProvider, locationClientConfig)
            locationClient.disableFilter(TimeLocationFilter())
            locationClient.disableFilter(DistanceLocationFilter())
            locationClient.disableFilter(AccuracyLocationFilter())
            verify {
                anyConstructed<AWSKeyValueStore>().put(
                    StoreKey.IS_ACCURACY_FILTER_ENABLE,
                    false.toString()
                )
            }
        }
    }

    @Test
    fun testStartBackground_ActiveTracking() {
        val locationClient =
            LocationTracker(context, locationCredentialsProvider, locationClientConfig)

        locationClient.startBackground(BackgroundTrackingMode.ACTIVE_TRACKING, serviceCallback)
        assertEquals(serviceCallback, BackgroundLocationService.serviceCallback)
    }

    @Test
    fun testStartBackground_InactiveTracking() {
        val locationClient =
            LocationTracker(context, locationCredentialsProvider, locationClientConfig)
        mockkObject(BackgroundTrackingWorker)

        every { BackgroundTrackingWorker.enqueueWork(context) } just runs

        locationClient.startBackground(
            BackgroundTrackingMode.BATTERY_SAVER_TRACKING,
            serviceCallback
        )
        verify { BackgroundTrackingWorker.enqueueWork(context) }
    }

    @Test
    fun `stopBackgroundService stops service and cancels work if running`() {
        mockkObject(BackgroundTrackingWorker)
        every { BackgroundTrackingWorker.isWorkRunning(context) } returns true
        every { BackgroundTrackingWorker.cancelWork(context) } just runs

        val locationClient =
            LocationTracker(context, locationCredentialsProvider, locationClientConfig)

        locationClient.stopBackgroundService()
        verify { BackgroundTrackingWorker.cancelWork(context) }
    }
}
