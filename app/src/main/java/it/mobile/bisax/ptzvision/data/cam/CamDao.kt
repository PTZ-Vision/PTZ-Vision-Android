package it.mobile.bisax.ptzvision.data.cam

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

@Dao
interface CamDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: Cam)

    @Update
    suspend fun update(item: Cam)

    @Delete
    suspend fun delete(item: Cam)
}
