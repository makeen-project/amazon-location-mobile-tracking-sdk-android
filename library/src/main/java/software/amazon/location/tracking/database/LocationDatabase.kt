package software.amazon.location.tracking.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Database class that provides access to the location entries in persistent storage.
 */
@Database(entities = [LocationEntry::class], version = 1)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun locationEntryDao(): LocationEntryDao
}