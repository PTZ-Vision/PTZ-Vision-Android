package it.mobile.bisax.ptzvision.data.cam

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CamDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(camera: Cam): Long

    @Delete
    suspend fun delete(camera: Cam)

    @Update
    suspend fun update(camera: Cam): Int

    //get all active cameras
    @Query("SELECT * FROM cam WHERE active = 1")
    fun getActive(): Flow<List<Cam>>

    //get all cameras
    @Query("SELECT * FROM cam")
    fun getAllCams(): Flow<List<Cam>>

    //get camera by id
    @Query("SELECT * FROM cam WHERE id = :id")
    fun getCam(id: Int): Flow<Cam?>

    @Query("UPDATE cam SET active = 0 WHERE id = :id")
    suspend fun removeActive(id: Int): Int

    @Query("UPDATE cam SET active = 1 WHERE id = :id AND (SELECT COUNT(*) FROM cam WHERE active = 1) < 4")
    suspend fun addActive(id: Int): Int

    @Query("SELECT COUNT(*) FROM cam WHERE active = 1")
    suspend fun countActiveCams(): Int
}
