package software.amazon.location.tracking

import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.room.RoomDatabase
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.amazonaws.internal.keyvaluestore.AWSKeyValueStore
import com.amazonaws.services.geo.AmazonLocationClient
import com.amazonaws.services.geo.model.BatchUpdateDevicePositionResult
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.gson.GsonBuilder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import software.amazon.location.auth.LocationCredentialsProvider
import software.amazon.location.tracking.TestConstants.TEST_CLIENT_CONFIG
import software.amazon.location.tracking.TestConstants.TEST_IDENTITY_POOL_ID
import software.amazon.location.tracking.TestConstants.TEST_LATITUDE
import software.amazon.location.tracking.TestConstants.TEST_LONGITUDE
import software.amazon.location.tracking.aws.AmazonTrackingHttpClient
import software.amazon.location.tracking.config.LocationTrackerConfig
import software.amazon.location.tracking.database.LocationEntry
import software.amazon.location.tracking.database.LocationEntryDao_Impl
import software.amazon.location.tracking.filters.TimeLocationFilter
import software.amazon.location.tracking.providers.BackgroundTrackingWorker
import software.amazon.location.tracking.providers.LocationProvider
import software.amazon.location.tracking.util.StoreKey
import software.amazon.location.tracking.util.TrackingSdkLogLevel
import java.util.UUID
import kotlin.concurrent.thread
import kotlin.test.assertNotNull


class BackgroundTrackingWorkerTest {

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var workerParameters: WorkerParameters

    private lateinit var backgroundTrackingWorker: BackgroundTrackingWorker
    private lateinit var locationClientConfig: LocationTrackerConfig
    private lateinit var locationCredentialsProvider: LocationCredentialsProvider
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var gsonBuilderMock: GsonBuilder
    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        workerParameters = mockk<WorkerParameters>(relaxed = true)
        mockkConstructor(AWSKeyValueStore::class)
        every { anyConstructed<AWSKeyValueStore>().get("apiKey") } returns "testApiKey"
        every { anyConstructed<AWSKeyValueStore>().get("region") } returns "us-east-1"
        every { anyConstructed<AWSKeyValueStore>().put(any(), any<String>()) } just runs
        every { anyConstructed<AWSKeyValueStore>().clear() } just runs
        locationCredentialsProvider = mockk()
        locationClientConfig = LocationTrackerConfig(
            trackerName = TestConstants.TRACKER_NAME,
            logLevel = TrackingSdkLogLevel.DEBUG,
            accuracy = Priority.PRIORITY_HIGH_ACCURACY,
            latency = 1000,
            frequency = 5000,
            waitForAccurateLocation = false,
            minUpdateIntervalMillis = 5000,
        )
        val locationClientConfig = mockk<LocationTrackerConfig>()
        every { locationClientConfig.logLevel } returns TrackingSdkLogLevel.DEBUG
        every { locationClientConfig.locationFilters } returns mutableListOf(TimeLocationFilter())
        every { locationClientConfig.trackerName } returns TestConstants.TRACKER_NAME

        gsonBuilderMock = mockk()
        every { gsonBuilderMock.create() } returns mockk()
        every { gsonBuilderMock.registerTypeAdapter(any(), any()) } returns gsonBuilderMock

        mockkConstructor(AmazonTrackingHttpClient::class)
        mockkConstructor(LocationProvider::class)
        mockkConstructor(RoomDatabase::class)
        mockkConstructor(AmazonLocationClient::class)
        mockkConstructor(LocationCredentialsProvider::class)
        every { locationCredentialsProvider.getCredentialsProvider() } returns mockk()
        fusedLocationProviderClient = mockk()
        mockkStatic(LocationServices::class)
        every { LocationServices.getFusedLocationProviderClient(context) } returns fusedLocationProviderClient

        mockkStatic(Build.VERSION::class)
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
        mockkStatic(Looper::class)
        val mainLooper = mockk<Looper>()
        every { Looper.getMainLooper() } returns mainLooper

        val mainThread = thread { }
        every { mainLooper.thread } returns mainThread


        mockkConstructor(Location::class)
        mockkConstructor(LocationTracker::class)
        every { anyConstructed<Location>().time } returns System.currentTimeMillis()
        every { anyConstructed<Location>().longitude } returns TEST_LONGITUDE
        every { anyConstructed<Location>().latitude } returns TEST_LATITUDE

        val location1 = LocationEntry(1, 10.0, 20.0, System.currentTimeMillis(), 5.0f)
        val locations = listOf(location1)
        mockkConstructor(LocationEntryDao_Impl::class)
        coEvery { anyConstructed<LocationEntryDao_Impl>().getAllEntries() } returns locations
        coEvery { anyConstructed<LocationEntryDao_Impl>().insert(any()) } just runs
        coEvery { anyConstructed<LocationEntryDao_Impl>().deleteEntriesByIds(any()) } just runs
        coEvery { anyConstructed<LocationEntryDao_Impl>().deleteEntryById(any()) } just runs
        val mockBatchUpdateDevicePositionResult = mockk<BatchUpdateDevicePositionResult>()
        coEvery {
            anyConstructed<AmazonLocationClient>().batchUpdateDevicePosition(any())
        } returns mockBatchUpdateDevicePositionResult
    }

    @Test
    fun testDoWork() {
        val location = mock(Location::class.java)
        `when`(location.latitude).thenReturn(TEST_LATITUDE)
        `when`(location.longitude).thenReturn(TEST_LONGITUDE)
        every { context.applicationContext } returns mockk()
        every { anyConstructed<AWSKeyValueStore>().get("method") } returns "cognito"
        every { anyConstructed<AWSKeyValueStore>().get("identityPoolId") } returns TEST_IDENTITY_POOL_ID
        every { anyConstructed<AWSKeyValueStore>().get(StoreKey.CLIENT_CONFIG) } returns TEST_CLIENT_CONFIG
        runBlocking {
            every { runBlocking { anyConstructed<LocationTracker>().getDeviceLocation(any()) } } returns location
            backgroundTrackingWorker = BackgroundTrackingWorker(context, workerParameters)
            val result = backgroundTrackingWorker.doWork()
            assertNotNull(result)
        }
    }

    @Test
    fun testEnqueueWork() {
        mockkStatic(WorkManager::class)
        every {
            WorkManager.getInstance(context).enqueue(ofType(PeriodicWorkRequest::class))
        } returns mockk()
        runBlocking {
            val result = BackgroundTrackingWorker.enqueueWork(context)
            assertNotNull(result)
        }
    }

    @Test
    fun testCancelWork() {
        mockkStatic(WorkManager::class)
        every {
            WorkManager.getInstance(context).cancelAllWorkByTag(any())
        } returns mockk()
        runBlocking {
            val result = BackgroundTrackingWorker.cancelWork(context)
            assertNotNull(result)
        }
    }

    @Test
    fun isWorkRunning() {
        mockkStatic(WorkManager::class)
        val mockWorkInfoList = listOf(
            createWorkInfo(WorkInfo.State.RUNNING),
        )
        every { WorkManager.getInstance(context).getWorkInfosByTag(any()).get() } returns mockWorkInfoList

        runBlocking {
            val result = BackgroundTrackingWorker.isWorkRunning(context)
            assertNotNull(result)
        }
    }

    private fun createWorkInfo(state: WorkInfo.State): WorkInfo {
        val id = UUID.randomUUID()
        return WorkInfo(
            id = id,
            state = state,
            tags = emptySet(),
            outputData = Data.EMPTY,
            progress = Data.EMPTY,
            runAttemptCount = 1,
            generation = 0,
            constraints = Constraints.NONE,
            initialDelayMillis = 0,
            periodicityInfo = null,
            nextScheduleTimeMillis = 0,
            stopReason = WorkInfo.STOP_REASON_APP_STANDBY
        )
    }
    @After
    fun tearDown() {
        unmockkAll()
    }
}
