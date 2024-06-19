package software.amazon.location.tracking

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import software.amazon.location.tracking.TestConstants.TEST_CLIENT_CONFIG
import software.amazon.location.tracking.providers.BackgroundLocationService
import software.amazon.location.tracking.util.StoreKey
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import software.amazon.location.auth.EncryptedSharedPreferences

class BackgroundLocationServiceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        mockkConstructor(NotificationCompat.Builder::class)
        mockkConstructor(EncryptedSharedPreferences::class)
        mockkConstructor(Build::class)
        every { anyConstructed<EncryptedSharedPreferences>().get(StoreKey.CLIENT_CONFIG) } returns TEST_CLIENT_CONFIG
        every { anyConstructed<EncryptedSharedPreferences>().initEncryptedSharedPreferences() } just runs
    }

    @Test
    fun testOnStartCommand() {
        val mockBuilder = mockk<NotificationCompat.Builder>(relaxed = true)
        every { anyConstructed<NotificationCompat.Builder>().setSmallIcon(any<Int>()) } returns mockBuilder
        every { anyConstructed<NotificationCompat.Builder>().setContentTitle(any()) } returns mockBuilder
        every { anyConstructed<NotificationCompat.Builder>().setContentText(any()) } returns mockBuilder
        every { anyConstructed<NotificationCompat.Builder>().setContentIntent(any()) } returns mockBuilder
        val service = BackgroundLocationService()
        val intent = Intent(context, BackgroundLocationService::class.java)
        val result = service.onStartCommand(intent, 0, 0)
        assert(result == Service.START_STICKY)
    }

    @Test
    fun testOnStartCommand_StopServiceAction() {
        val service = BackgroundLocationService()
        val intent = mock(Intent::class.java)
        `when`(intent.action).thenReturn("STOP_SERVICE_ACTION")

        val result = service.onStartCommand(intent, 0, 0)

        assert(result == Service.START_NOT_STICKY)
    }

    @Test
    fun onDestroy() {
        val service = BackgroundLocationService()
        service.onDestroy()
        assertFalse(BackgroundLocationService.isRunning)
    }

    @Test
    fun onBindAndCreate() {
        val service = BackgroundLocationService()
        val intent = Intent(context, BackgroundLocationService::class.java)
        service.onBind(intent)
        service.onCreate()
        assertTrue(BackgroundLocationService.isRunning)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }
}