package it.mobile.bisax.ptzvision.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import it.mobile.bisax.ptzvision.data.cam.Cam
import it.mobile.bisax.ptzvision.data.cam.CamDao
import it.mobile.bisax.ptzvision.data.preset.Preset
import it.mobile.bisax.ptzvision.data.scene.Scene
import it.mobile.bisax.ptzvision.data.scene.SceneDao

@Database(entities = [Scene::class, Cam::class, Preset::class], version = 4, exportSchema = false)
abstract class PTZDatabase : RoomDatabase() {

    abstract fun sceneDao(): SceneDao
    abstract fun camDao(): CamDao

    companion object {
        @Volatile
        private var Instance: PTZDatabase? = null

        fun getDatabase(context: Context): PTZDatabase {

            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, PTZDatabase::class.java, "ptz_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}