package software.amazon.location.tracking

import android.content.Context
import com.amazonaws.internal.keyvaluestore.AWSKeyValueStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import software.amazon.location.tracking.providers.DeviceIdProvider

class DeviceIdProviderTest {

    private lateinit var context: Context
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private lateinit var deviceIdProvider: DeviceIdProvider


    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        deviceIdProvider = DeviceIdProvider.getInstance(context)
        mockkConstructor(AWSKeyValueStore::class)
        every { anyConstructed<AWSKeyValueStore>().get(any()) } returns "mockDeviceID"
    }

    @Test
    fun `test the same device ID for the same app session`() {
        coroutineScope.launch {
            val deviceIdProvider = DeviceIdProvider.getInstance(context)

            // Get the initial device ID
            val initialDeviceID = deviceIdProvider.getDeviceID()

            // Get the device ID again within the same session
            val secondDeviceID = deviceIdProvider.getDeviceID()

            // Make sure the initial and second device IDs are the same
            assertNotNull(initialDeviceID)
            assertNotNull(secondDeviceID)
            assertEquals(initialDeviceID, secondDeviceID)
        }
    }

    @Test
    fun `test different device IDs for different app sessions`() {
        coroutineScope.launch {
            val deviceIdProvider = DeviceIdProvider.getInstance(context)

            // Get the initial device ID
            val initialDeviceID = deviceIdProvider.getDeviceID()

            // Reset the device ID, for different session
            deviceIdProvider.resetDeviceID()

            // Get the device ID after resetting
            val resetDeviceID = deviceIdProvider.getDeviceID()

            // Make sure the initial and reset device IDs are different
            assertNotNull(initialDeviceID)
            assertNotNull(resetDeviceID)
            assertNotEquals(initialDeviceID, resetDeviceID)
        }
    }

    @Test
    fun `test different device IDs for same users on the different device`() {
        coroutineScope.launch {
            // User A logs into Device 1
            val userADevice1Provider = DeviceIdProvider.getInstance(context)
            val userADevice1ID = userADevice1Provider.getDeviceID()

            // Reset the device ID for different device
            userADevice1Provider.resetDeviceID()

            // User A logs into Device 2
            val userADevice2Provider = DeviceIdProvider.getInstance(context)
            val userADevice2ID = userADevice2Provider.getDeviceID()

            // User A should have different device IDs on different devices
            assertNotNull(userADevice1ID)
            assertNotNull(userADevice2ID)
            assertNotEquals(userADevice1ID, userADevice2ID)
        }
    }

    @Test
    fun `test different device IDs for different users on the same device`() {
        coroutineScope.launch {
            // User A logs into Device 1
            val userADevice1Provider = DeviceIdProvider.getInstance(context)
            val userADevice1ID = userADevice1Provider.getDeviceID()

            // when user1 logout
            userADevice1Provider.resetDeviceID()

            // User B logs into Device 1
            val userBDevice1Provider = DeviceIdProvider.getInstance(context)
            val userBDevice1ID = userBDevice1Provider.getDeviceID()

            // User A and User B should have different device IDs on the same device
            assertNotNull(userADevice1ID)
            assertNotNull(userBDevice1ID)
            assertNotEquals(userADevice1ID, userBDevice1ID)
        }
    }

    @Test
    fun `getDeviceID should return device ID from AWSKeyValueStore`() {
        coroutineScope.launch {
            val result = deviceIdProvider.getDeviceID()

            assertEquals("mockDeviceID", result)
        }
    }
}
