package software.amazon.location.tracking

import junit.framework.TestCase.assertEquals
import org.junit.Before
import software.amazon.location.tracking.util.PropertyKey
import software.amazon.location.tracking.util.StoreKey
import kotlin.test.Test

class StoreKeyAndPropertyKeyTest {

    private lateinit var storeKey: StoreKey
    private lateinit var propertyKey: PropertyKey

    @Before
    fun setUp(){
        storeKey = StoreKey
        propertyKey = PropertyKey
    }

    @Test
    fun `test default time interval`() {

        val timeInterval = storeKey.DEVICE_ID
        assertEquals(timeInterval, StoreKey.DEVICE_ID)
    }
}