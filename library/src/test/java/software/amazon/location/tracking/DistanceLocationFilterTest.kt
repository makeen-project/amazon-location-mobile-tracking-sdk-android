package software.amazon.location.tracking

import android.location.Location
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.runs
import software.amazon.location.tracking.database.LocationEntry
import software.amazon.location.tracking.filters.DistanceLocationFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DistanceLocationFilterTest {
    @Before
    fun setUp() {
        mockkConstructor(Location::class)
        every { anyConstructed<Location>().latitude = any() } just runs
        every { anyConstructed<Location>().longitude = any() } just runs
    }

    @Test
    fun `should upload when previous location is null`() {
        val filter = DistanceLocationFilter()
        val currentLocation = LocationEntry(
            1,
            TestConstants.latitude,
            TestConstants.longitude,
            System.currentTimeMillis(),
            TestConstants.accuracy,
        )
        val shouldUpload = filter.shouldUpload(currentLocation, null)
        assertTrue(shouldUpload)
    }

    @Test
    fun `should upload when distance exceeds threshold`() {
        val filter = DistanceLocationFilter(distanceThreshold = 100.0)
        val currentLocation = LocationEntry(
            1,
            1.0,
            2.0,
            System.currentTimeMillis(),
            TestConstants.accuracy,
        )
        val previousLocation = LocationEntry(
            2,
            10.0,
            20.0,
            System.currentTimeMillis(),
            TestConstants.accuracy,
        )
        val distanceTo = 105.0f
        every { anyConstructed<Location>().distanceTo(any()) } returns distanceTo
        val shouldUpload = filter.shouldUpload(currentLocation, previousLocation)
        assertTrue(shouldUpload)
    }

    @Test
    fun `should not upload when distance is within threshold`() {
        val filter = DistanceLocationFilter(distanceThreshold = 1000.0)
        val currentLocation = LocationEntry(
            1,
            TestConstants.latitude,
            TestConstants.longitude,
            System.currentTimeMillis(),
            TestConstants.accuracy,
        )
        val previousLocation = LocationEntry(
            2,
            TestConstants.latitude,
            TestConstants.longitude,
            System.currentTimeMillis(),
            TestConstants.accuracy,
        )
        val distanceTo = 5.0f
        every { anyConstructed<Location>().distanceTo(any()) } returns distanceTo
        val shouldUpload = filter.shouldUpload(currentLocation, previousLocation)
        assertFalse(shouldUpload)
    }
}
