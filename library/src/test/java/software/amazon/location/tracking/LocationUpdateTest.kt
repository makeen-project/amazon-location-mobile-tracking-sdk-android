package software.amazon.location.tracking

import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.amazonaws.AmazonClientException
import com.amazonaws.services.geo.AmazonLocationClient
import com.amazonaws.services.geo.model.BatchUpdateDevicePositionRequest
import com.amazonaws.services.geo.model.BatchUpdateDevicePositionResult
import com.amazonaws.services.geo.model.GetDevicePositionRequest
import com.amazonaws.services.geo.model.GetDevicePositionResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import software.amazon.location.tracking.aws.AmazonTrackingHttpClient
import software.amazon.location.tracking.config.SdkConfig.MAX_RETRY
import software.amazon.location.tracking.providers.DeviceIdProvider
import kotlin.test.assertNotNull

class LocationUpdateTest {
    private lateinit var context: Context
    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        mockkStatic(Log::class)

        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        mockkStatic(Looper::class)
    }

    @Test
    fun `test update Tracker Device Locations`() {
        val mockAmazonLocationClient = mockk<AmazonLocationClient>()
        val mockLocation = mockk<Location>()
        val mockDeviceIdProvider = mockk<DeviceIdProvider>()

        val deviceID = "mockDeviceID"
        every { mockDeviceIdProvider.getDeviceID() } returns deviceID

        val currentTimeMillis = System.currentTimeMillis()
        every { mockLocation.time } returns currentTimeMillis
        every { mockLocation.latitude } returns 23.455
        every { mockLocation.longitude } returns 103.556
        val exception = AmazonClientException("Mock exception")
        coEvery {
            mockAmazonLocationClient.batchUpdateDevicePosition(any())
        } throws exception

        val amazonTrackingHttpClient = AmazonTrackingHttpClient(context, "mockTrackerName")

        runBlocking(Dispatchers.Default) {
            try {
                amazonTrackingHttpClient.updateTrackerDeviceLocation(
                    mockAmazonLocationClient,
                    arrayOf(mockLocation)
                )
            } catch (e: AmazonClientException) {
                verify(exactly = MAX_RETRY) {
                    mockAmazonLocationClient.batchUpdateDevicePosition(any())
                }
            }
        }
    }

    @Test
    fun `test get Tracker Device Location`() {
        val mockAmazonLocationClient = mockk<AmazonLocationClient>()
        val mockDeviceIdProvider = mockk<DeviceIdProvider>()

        val deviceID = "mockDeviceID"
        every { mockDeviceIdProvider.getDeviceID() } returns deviceID

        val amazonTrackingHttpClient = AmazonTrackingHttpClient(context, "mockTrackerName")

        val mockGetDevicePositionResult = mockk<GetDevicePositionResult>()
        coEvery {
            mockAmazonLocationClient.getDevicePosition(any())
        } returns mockGetDevicePositionResult

        runBlocking(Dispatchers.IO) {
            val result = amazonTrackingHttpClient.getTrackerDeviceLocation(mockAmazonLocationClient)

            coEvery {
                mockAmazonLocationClient.getDevicePosition(
                    match<GetDevicePositionRequest> {
                        it.deviceId == deviceID && it.trackerName == "mockTrackerName"
                    }
                )
            }

            assertEquals(mockGetDevicePositionResult, result)
        }
    }

    @Test
    fun `test update Tracker Device single Location`() {
        val mockAmazonLocationClient = mockk<AmazonLocationClient>()
        val mockLocation = mockk<Location>()

        val mockLocationTime = 123456789L
        every { mockLocation.time } returns mockLocationTime
        every { mockLocation.latitude } returns 23.455
        every { mockLocation.longitude } returns 103.556

        val mockBatchUpdateDevicePositionResult = mockk<BatchUpdateDevicePositionResult>()
        coEvery {
            mockAmazonLocationClient.batchUpdateDevicePosition(any())
        } returns mockBatchUpdateDevicePositionResult

        val amazonTrackingHttpClient = AmazonTrackingHttpClient(context, "mockTrackerName")

        runBlocking(Dispatchers.Default) {
            val result = amazonTrackingHttpClient.updateTrackerDeviceLocation(
                mockAmazonLocationClient,
                mockLocation
            )

            coEvery {
                mockAmazonLocationClient.batchUpdateDevicePosition(
                    match<BatchUpdateDevicePositionRequest> {
                        it.updates.size == 1 && it.updates[0].sampleTime.time == mockLocationTime
                    }
                )
            }
            assertNotNull(result)
        }
    }
}
