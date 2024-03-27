package software.amazon.location.tracking

import android.location.Location
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.runs
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import software.amazon.location.tracking.database.LocationEntry
import software.amazon.location.tracking.filters.LocationFilter

class CustomDistanceLocationFilterTest {

    private val distanceThreshold = 1000.0

    @Before
    fun setUp() {
        mockkConstructor(Location::class)
        every { anyConstructed<Location>().latitude = any() } just runs
        every { anyConstructed<Location>().longitude = any() } just runs
    }

    @Test
    fun shouldUpload_WhenDistanceExceedsThreshold() {
        val centralPoint = Location("").apply {
            latitude = 0.01
            longitude = 0.01
        }
        val locationFarEnough = LocationEntry(
            id = 1,
            latitude = TestConstants.latitude,
            longitude = TestConstants.longitude,
            time = System.currentTimeMillis(),
            accuracy = TestConstants.accuracy
        )
        val customFilter = createDistanceFilter(centralPoint, distanceThreshold)
        val distanceTo = 1500.0f
        every { anyConstructed<Location>().distanceTo(any()) } returns distanceTo
        assertTrue(
            "Should upload when distance exceeds threshold",
            customFilter.shouldUpload(locationFarEnough, null)
        )
    }

    @Test
    fun shouldNotUpload_WhenDistanceBelowThreshold() {
        val centralPoint = Location("").apply {
            latitude = TestConstants.latitude
            longitude = TestConstants.longitude
        }

        val locationCloseEnough = LocationEntry(
            id = 0,
            latitude = TestConstants.latitude,
            longitude = TestConstants.longitude,
            time = System.currentTimeMillis(),
            accuracy = TestConstants.accuracy
        )
        val customFilter = createDistanceFilter(centralPoint, distanceThreshold)
        val distanceTo = 5.0f
        every { anyConstructed<Location>().distanceTo(any()) } returns distanceTo
        assertFalse(
            "Should not upload when distance is below threshold",
            customFilter.shouldUpload(locationCloseEnough, null)
        )
    }

    private fun createDistanceFilter(centralPoint: Location, threshold: Double): LocationFilter {
        return object : LocationFilter {
            override fun shouldUpload(
                currentLocation: LocationEntry, previousLocation: LocationEntry?
            ): Boolean {
                val currentLocationData = Location("LocationProvider").apply {
                    latitude = currentLocation.latitude
                    longitude = currentLocation.longitude
                }
                return currentLocationData.distanceTo(centralPoint).toDouble() > threshold
            }
        }
    }
}