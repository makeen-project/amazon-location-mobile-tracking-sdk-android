package software.amazon.location.tracking

import junit.framework.TestCase.assertEquals
import org.junit.Before
import software.amazon.location.tracking.config.SdkConfig
import kotlin.test.Test

class SdkConfigTest {

    private lateinit var sdkConfig: SdkConfig

    @Before
    fun setUp(){
        sdkConfig = SdkConfig
    }

    @Test
    fun `test default time interval`() {

        val timeInterval = sdkConfig.DEFAULT_TIME_INTERVAL
        assertEquals(timeInterval, SdkConfig.DEFAULT_TIME_INTERVAL)
    }

    @Test
    fun `test default distance threshold`() {
        val distanceThreshold = sdkConfig.DEFAULT_DISTANCE_THRESHOLD
        assertEquals(distanceThreshold, SdkConfig.DEFAULT_DISTANCE_THRESHOLD, 0.0)
    }

    @Test
    fun `test default accuracy`() {
        val accuracy = sdkConfig.DEFAULT_ACCURACY
        assertEquals(accuracy, SdkConfig.DEFAULT_ACCURACY, 0.0f)
    }

    @Test
    fun `test max retry`() {
        val maxRetry = sdkConfig.MAX_RETRY
        assertEquals(maxRetry, SdkConfig.MAX_RETRY)
    }
}