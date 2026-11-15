package com.nezuplay.video.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VideoMetadataEntity::class,
        PrivateVideoEntity::class,
        UserPreferenceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NezuPlayDatabase : RoomDatabase() {
    abstract fun videoMetadataDao(): VideoMetadataDao
    abstract fun privateVideoDao(): PrivateVideoDao
    abstract fun userPreferencesDao(): UserPreferencesDao

    companion object {
        @Volatile
        private var INSTANCE: NezuPlayDatabase? = null

        fun getInstance(context: Context): NezuPlayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NezuPlayDatabase::class.java,
                    "nezuplay_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
