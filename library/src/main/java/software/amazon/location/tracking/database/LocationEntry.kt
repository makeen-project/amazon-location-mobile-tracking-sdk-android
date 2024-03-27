package software.amazon.location.tracking.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a location entry in the database.
 *
 * @property id The unique identifier of the location entry.
 * @property latitude The latitude of the location.
 * @property longitude The longitude of the location.
 * @property time The time at which the location was recorded.
 * @property accuracy The accuracy of the location.
 */
@Entity(tableName = "location_entries")
data class LocationEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val time: Long,
    val accuracy: Float,
)