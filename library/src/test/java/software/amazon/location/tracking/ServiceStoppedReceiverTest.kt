package software.amazon.location.tracking

import android.content.Context
import android.content.Intent
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import software.amazon.location.tracking.providers.ServiceStoppedReceiver

@RunWith(MockitoJUnitRunner::class)
class ServiceStoppedReceiverTest {

    @Mock
    lateinit var mockListener: ServiceStoppedReceiver.ServiceStoppedListener

    @Test
    fun testOnReceive() {
        val mockContext = mock(Context::class.java)
        val mockIntent = mock(Intent::class.java)
        val receiver = ServiceStoppedReceiver(mockListener)
        receiver.onReceive(mockContext, mockIntent)

        verify(mockListener).onServiceStopped()
    }
}
