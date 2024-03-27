package software.amazon.location.tracking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import software.amazon.location.tracking.database.LocationEntry
import software.amazon.location.tracking.filters.LocationFilter
import java.util.Calendar

class CustomTimeLocationFilterTest {

    @Test
    fun shouldUpload_WhenDayIsMonday() {
        val mondayLocation = createLocationEntryWithSpecificDay(Calendar.MONDAY)
        val customFilter = createTimeFilter()

        assertTrue("Should upload on Monday", customFilter.shouldUpload(mondayLocation, null))
    }

    @Test
    fun shouldNotUpload_WhenDayIsNotMonday() {
        val daysOfWeek = listOf(
            Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
            Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        )

        val customFilter = createTimeFilter()
        daysOfWeek.forEach { day ->
            val location = createLocationEntryWithSpecificDay(day)
            assertFalse(
                "Should not upload when day is not Monday (Day: $day)",
                customFilter.shouldUpload(location, null)
            )
        }
    }

    private fun createLocationEntryWithSpecificDay(dayOfWeek: Int): LocationEntry {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
        }
        return LocationEntry(
            id = 0,
            latitude = TestConstants.latitude,
            longitude = TestConstants.longitude,
            time = calendar.timeInMillis,
            accuracy = TestConstants.accuracy
        )
    }

    private fun createTimeFilter(): LocationFilter {
        return object : LocationFilter {
            override fun shouldUpload(
                currentLocation: LocationEntry,
                previousLocation: LocationEntry?
            ): Boolean {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = currentLocation.time
                val dayOfWeek = calendar[Calendar.DAY_OF_WEEK]
                return dayOfWeek == Calendar.MONDAY
            }
        }
    }
}