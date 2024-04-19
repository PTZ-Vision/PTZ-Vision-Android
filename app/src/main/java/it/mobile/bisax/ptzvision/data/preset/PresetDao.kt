package it.mobile.bisax.ptzvision.data.preset

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

@Dao
interface PresetDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: Preset)

    @Update
    suspend fun update(item: Preset)

    @Delete
    suspend fun delete(item: Preset)
}