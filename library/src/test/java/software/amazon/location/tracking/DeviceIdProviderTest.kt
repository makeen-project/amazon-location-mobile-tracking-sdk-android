package software.amazon.location.tracking

import android.content.Context
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import software.amazon.location.auth.EncryptedSharedPreferences
import software.amazon.location.tracking.providers.DeviceIdProvider
import software.amazon.location.tracking.util.StoreKey

class DeviceIdProviderTest {

    private lateinit var context: Context
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private lateinit var deviceIdProvider: DeviceIdProvider


    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        mockkConstructor(EncryptedSharedPreferences::class)
        every { anyConstructed<EncryptedSharedPreferences>().initEncryptedSharedPreferences() } just runs
        every { anyConstructed<EncryptedSharedPreferences>().clear() } just runs
        every { anyConstructed<EncryptedSharedPreferences>().get(any()) } returns "mockDeviceID"
        every { anyConstructed<EncryptedSharedPreferences>().contains(StoreKey.DEVICE_ID) } returns true
        deviceIdProvider = DeviceIdProvider.getInstance(context)
    }

    @Test
    fun `getDeviceID should return device ID from AWSKeyValueStore`() {
        coroutineScope.launch {
            val result = deviceIdProvider.getDeviceID()

            assertEquals("mockDeviceID", result)
        }
    }
}
