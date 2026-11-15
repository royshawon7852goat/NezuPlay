package com.nezuplay.video.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoMetadataDao {
    @Query("SELECT * FROM video_metadata")
    fun getAllMetadataFlow(): Flow<List<VideoMetadataEntity>>

    @Query("SELECT * FROM video_metadata WHERE uriString = :uriString LIMIT 1")
    suspend fun getMetadata(uriString: String): VideoMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(metadata: VideoMetadataEntity)

    @Query("UPDATE video_metadata SET isFavorite = :isFavorite WHERE uriString = :uriString")
    suspend fun updateFavorite(uriString: String, isFavorite: Boolean)

    @Query("UPDATE video_metadata SET lastPositionMs = :position, lastPlayedTimestamp = :timestamp, playCount = playCount + 1, durationMs = :duration WHERE uriString = :uriString")
    suspend fun updatePlayback(uriString: String, position: Long, duration: Long, timestamp: Long)

    @Query("UPDATE video_metadata SET lastPositionMs = 0, lastPlayedTimestamp = 0")
    suspend fun clearHistory()

    @Query("DELETE FROM video_metadata WHERE uriString = :uriString")
    suspend fun deleteMetadata(uriString: String)
}

@Dao
interface PrivateVideoDao {
    @Query("SELECT * FROM private_videos ORDER BY importTimestamp DESC")
    fun getAllPrivateVideosFlow(): Flow<List<PrivateVideoEntity>>

    @Query("SELECT * FROM private_videos WHERE id = :id LIMIT 1")
    suspend fun getPrivateVideoById(id: Long): PrivateVideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrivateVideo(video: PrivateVideoEntity): Long

    @Update
    suspend fun updatePrivateVideo(video: PrivateVideoEntity)

    @Delete
    suspend fun deletePrivateVideo(video: PrivateVideoEntity)

    @Query("SELECT COUNT(*) FROM private_videos")
    suspend fun getPrivateVideoCount(): Int

    @Query("SELECT SUM(sizeBytes) FROM private_videos")
    suspend fun getTotalPrivateSize(): Long?
}

@Dao
interface UserPreferencesDao {
    @Query("SELECT value FROM user_preferences WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Query("SELECT value FROM user_preferences WHERE `key` = :key LIMIT 1")
    fun getValueFlow(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setValue(entity: UserPreferenceEntity)

    @Query("DELETE FROM user_preferences WHERE `key` = :key")
    suspend fun removeValue(key: String)
}
