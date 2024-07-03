package software.amazon.location.tracking

import android.content.Context
import android.location.Location
import aws.sdk.kotlin.services.location.LocationClient
import aws.sdk.kotlin.services.location.model.BatchEvaluateGeofencesResponse
import aws.sdk.kotlin.services.location.model.BatchUpdateDevicePositionResponse
import aws.sdk.kotlin.services.location.model.GetDevicePositionResponse
import io.mockk.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import software.amazon.location.auth.EncryptedSharedPreferences
import software.amazon.location.tracking.aws.AmazonTrackingHttpClient
import software.amazon.location.tracking.database.LocationEntry
import software.amazon.location.tracking.providers.DeviceIdProvider
import software.amazon.location.tracking.util.StoreKey

class AmazonTrackingHttpClientTest {
    private lateinit var context: Context
    private lateinit var deviceIdProvider: DeviceIdProvider
    private lateinit var locationClient: LocationClient
    private lateinit var amazonTrackingHttpClient: AmazonTrackingHttpClient

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        deviceIdProvider = mockk(relaxed = true)
        locationClient = mockk(relaxed = true)
        mockkConstructor(DeviceIdProvider::class)
        mockkConstructor(EncryptedSharedPreferences::class)
        every { anyConstructed<EncryptedSharedPreferences>().initEncryptedSharedPreferences() } just runs
        every { anyConstructed<EncryptedSharedPreferences>().get(any()) } returns "mockDeviceID"
        every { anyConstructed<EncryptedSharedPreferences>().contains(StoreKey.DEVICE_ID) } returns true
        every { anyConstructed<EncryptedSharedPreferences>().put(any(), any<String>()) } just runs
        every { anyConstructed<DeviceIdProvider>().getDeviceID() } returns "mockDeviceID"

        amazonTrackingHttpClient = AmazonTrackingHttpClient(context, "mockTracker")
    }

    @Test
    fun `test updateTrackerDeviceLocation with single location`() {
        val mockDeviceIdProvider = mockk<DeviceIdProvider>()

        val deviceID = "mockDeviceID"
        every { mockDeviceIdProvider.getDeviceID() } returns deviceID
        val location = mockk<Location>(relaxed = true)
        val response = mockk<BatchUpdateDevicePositionResponse>(relaxed = true)

        coEvery { locationClient.batchUpdateDevicePosition(any()) } returns response
        runBlocking {
            val result =
                amazonTrackingHttpClient.updateTrackerDeviceLocation(locationClient, location)

            assertNotNull(result)
            coVerify { locationClient.batchUpdateDevicePosition(any()) }
        }
    }

    @Test
    fun `test updateTrackerDeviceLocation with multiple locations`() {
        val locations = arrayOf(mockk<Location>(relaxed = true), mockk<Location>(relaxed = true))
        val response = mockk<BatchUpdateDevicePositionResponse>(relaxed = true)

        coEvery { locationClient.batchUpdateDevicePosition(any()) } returns response

        runBlocking {
            val result =
                amazonTrackingHttpClient.updateTrackerDeviceLocation(locationClient, locations)

            assertNotNull(result)
            coVerify { locationClient.batchUpdateDevicePosition(any()) }
        }
    }

    @Test
    fun `test getTrackerDeviceLocation`() {
        val response = mockk<GetDevicePositionResponse>(relaxed = true)

        coEvery { locationClient.getDevicePosition(any()) } returns response

        runBlocking {
            val result = amazonTrackingHttpClient.getTrackerDeviceLocation(locationClient)

            assertNotNull(result)
            coVerify { locationClient.getDevicePosition(any()) }
        }
    }

    @Test
    fun `test batchEvaluateGeofences`() {
        val locationEntries =
            listOf(mockk<LocationEntry>(relaxed = true), mockk<LocationEntry>(relaxed = true))
        val response = mockk<BatchEvaluateGeofencesResponse>(relaxed = true)

        coEvery { locationClient.batchEvaluateGeofences(any()) } returns response
        runBlocking {
            val result =
                amazonTrackingHttpClient.batchEvaluateGeofences(
                    locationClient,
                    locationEntries,
                    "mockDeviceID",
                    "region:id",
                    "geofenceCollection",
                )

            assertNotNull(result)
            coVerify { locationClient.batchEvaluateGeofences(any()) }
        }
    }

    @Test
    fun `test updateTrackerDeviceLocation with multiple locations and exception`() {
        val locations = arrayOf(mockk<Location>(relaxed = true), mockk<Location>(relaxed = true))

        coEvery { locationClient.batchUpdateDevicePosition(any()) } throws Exception("Network error")
        runBlocking {
            var result: BatchUpdateDevicePositionResponse? = null
            try {
                result =
                    amazonTrackingHttpClient.updateTrackerDeviceLocation(locationClient, locations)
            } catch (e: Exception) {
                assertEquals("Network error", e.message)
            }

            assertNull(result)

            coVerify(atLeast = 3) { locationClient.batchUpdateDevicePosition(any()) }
        }
    }
}
