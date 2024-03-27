package software.amazon.location.tracking.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Interface that defines the Data Access Object (DAO) for LocationEntry entities in the database.
 *
 * This DAO provides methods to perform CRUD operations (insert, retrieve) on LocationEntry entities,
 * as well as querying for entries with specific properties.
 *
 */
@Dao
interface LocationEntryDao {
    @Insert
    suspend fun insert(entry: LocationEntry)

    @Query("SELECT * FROM location_entries")
    suspend fun getAllEntries(): List<LocationEntry>

    @Query("DELETE FROM location_entries")
    suspend fun deleteAll()

    @Query("DELETE FROM location_entries WHERE id IN (:entryIds)")
    suspend fun deleteEntriesByIds(entryIds: List<Long>)

    @Query("DELETE FROM location_entries WHERE id = :entryId")
    suspend fun deleteEntryById(entryId: Long)
}