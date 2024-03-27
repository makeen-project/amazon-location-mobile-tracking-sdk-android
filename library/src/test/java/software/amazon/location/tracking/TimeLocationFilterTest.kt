package software.amazon.location.tracking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import software.amazon.location.tracking.TestConstants.accuracy
import software.amazon.location.tracking.TestConstants.latitude
import software.amazon.location.tracking.TestConstants.longitude
import software.amazon.location.tracking.TestConstants.timeInterval
import software.amazon.location.tracking.database.LocationEntry
import software.amazon.location.tracking.filters.TimeLocationFilter
import java.util.concurrent.TimeUnit

class TimeLocationFilterTest {
    @Test
    fun `should upload when previous location is null`() {
        val timeLocationFilter = TimeLocationFilter(timeInterval)

        val currentLocation =
            LocationEntry(1, latitude, longitude, System.currentTimeMillis(), accuracy)
        val result = timeLocationFilter.shouldUpload(currentLocation, null)

        assertTrue(result)
    }

    @Test
    fun `should upload when time difference exceeds the interval`() {
        val timeLocationFilter = TimeLocationFilter(timeInterval)

        val currentTime = System.currentTimeMillis()
        val previousTime = currentTime - TimeUnit.SECONDS.toMillis(2) // 2 seconds ago

        val currentLocation = LocationEntry(1, latitude, longitude, currentTime, accuracy)
        val previousLocation = LocationEntry(2, latitude, longitude, previousTime, accuracy)
        val result = timeLocationFilter.shouldUpload(currentLocation, previousLocation)

        assertTrue(result)
    }

    @Test
    fun `should not upload when time difference is within the interval`() {
        val timeLocationFilter = TimeLocationFilter(timeInterval)

        val currentTime = System.currentTimeMillis()
        val previousTime = currentTime - TimeUnit.MILLISECONDS.toMillis(500) // 0.5 seconds ago

        val currentLocation = LocationEntry(1, latitude, longitude, currentTime, accuracy)
        val previousLocation = LocationEntry(2, latitude, longitude, previousTime, accuracy)
        val result = timeLocationFilter.shouldUpload(currentLocation, previousLocation)

        assertFalse(result)
    }
}
