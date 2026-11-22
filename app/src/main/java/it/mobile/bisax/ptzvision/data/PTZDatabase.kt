package it.mobile.bisax.ptzvision.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import it.mobile.bisax.ptzvision.data.cam.Cam
import it.mobile.bisax.ptzvision.data.cam.CamDao

@Database(entities = [Cam::class], version = 11, exportSchema = false)
abstract class PTZDatabase : RoomDatabase() {
    abstract fun camDao(): CamDao

    companion object {
        @Volatile
        private var Instance: PTZDatabase? = null

        /** v11: NDI support (protocol + NDI source name), unique index extended with the NDI name. */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `cam` ADD COLUMN `protocol` TEXT NOT NULL DEFAULT 'VISCA'")
                db.execSQL("ALTER TABLE `cam` ADD COLUMN `ndiSourceName` TEXT NOT NULL DEFAULT ''")
                db.execSQL("DROP INDEX IF EXISTS `index_cam_ip_controlPort_streamPort_httpPort`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_cam_ip_controlPort_streamPort_httpPort_ndiSourceName` " +
                        "ON `cam` (`ip`, `controlPort`, `streamPort`, `httpPort`, `ndiSourceName`)"
                )
            }
        }

        fun getDatabase(context: Context): PTZDatabase {

            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, PTZDatabase::class.java, "ptz_database")
                    .addMigrations(MIGRATION_10_11)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
