package it.mobile.bisax.ptzvision.data.scene

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SceneDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(scene: Scene)

    @Update
    suspend fun update(scene: Scene)

    @Delete
    suspend fun delete(scene: Scene)

    @Query("SELECT * from scene WHERE id = :id")
    fun getItem(id: Int): Flow<Scene>

    @Query("SELECT * from scene ORDER BY name ASC")
    fun getAllItems(): Flow<List<Scene>>
}