package it.mobile.bisax.ptzvision.data.scene

import androidx.room.Dao
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

    @Query("SELECT * FROM scene WHERE idCamera=:camId ORDER BY slot ASC")
    fun getScenesbyCam(camId: Int): Flow<List<Scene>>
}