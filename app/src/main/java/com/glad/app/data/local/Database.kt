package com.glad.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

object AnimeStatus {
    const val WATCHING = "WATCHING"
    const val PLAN = "PLAN"
    const val COMPLETED = "COMPLETED"
    const val ON_HOLD = "ON_HOLD"
    const val DROPPED = "DROPPED"
    val all = listOf(WATCHING, PLAN, COMPLETED, ON_HOLD, DROPPED)

    fun label(status: String) = when (status) {
        WATCHING -> "Watching"
        PLAN -> "Plan to watch"
        COMPLETED -> "Completed"
        ON_HOLD -> "On hold"
        DROPPED -> "Dropped"
        else -> status
    }
}

object MovieStatus {
    const val WATCHLIST = "WATCHLIST"
    const val WATCHED = "WATCHED"
}

@Entity(tableName = "favorite_wallpapers")
data class FavoriteWallpaper(
    @PrimaryKey val wallpaperKey: String,
    val title: String,
    val addedAt: Long
)

@Entity(tableName = "anime_entries")
data class AnimeEntry(
    @PrimaryKey val malId: Int,
    val title: String,
    val imageUrl: String?,
    val totalEpisodes: Int,
    val watched: Int,
    val status: String,
    val score: Int,
    val updatedAt: Long
)

@Entity(tableName = "movies")
data class MovieEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val year: Int?,
    val genre: String,
    val notes: String,
    val status: String,
    val rating: Int,
    val addedAt: Long,
    val watchedAt: Long?
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [ForeignKey(
        entity = Playlist::class,
        parentColumns = ["id"],
        childColumns = ["playlistId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("playlistId")]
)
data class PlaylistTrack(
    val playlistId: Long,
    val trackId: Long,
    val addedAt: Long
)

@Dao
interface WallpaperDao {
    @Query("SELECT * FROM favorite_wallpapers ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteWallpaper>>

    @Query("SELECT COUNT(*) FROM favorite_wallpapers")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM favorite_wallpapers WHERE wallpaperKey = :key")
    fun observeIsFavorite(key: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(item: FavoriteWallpaper)

    @Query("DELETE FROM favorite_wallpapers WHERE wallpaperKey = :key")
    suspend fun remove(key: String)
}

@Dao
interface AnimeDao {
    @Query("SELECT * FROM anime_entries ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<AnimeEntry>>

    @Query("SELECT * FROM anime_entries WHERE malId = :id")
    fun observe(id: Int): Flow<AnimeEntry?>

    @Query("SELECT * FROM anime_entries WHERE status = 'WATCHING' ORDER BY updatedAt DESC LIMIT 6")
    fun observeContinueWatching(): Flow<List<AnimeEntry>>

    @Query("SELECT COUNT(*) FROM anime_entries WHERE status = 'WATCHING'")
    fun observeWatchingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM anime_entries WHERE status = 'WATCHING'")
    suspend fun countWatching(): Int

    @Upsert
    suspend fun upsert(entry: AnimeEntry)

    @Query("DELETE FROM anime_entries WHERE malId = :id")
    suspend fun delete(id: Int)
}

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<MovieEntry>>

    @Query("SELECT COUNT(*) FROM movies WHERE status = 'WATCHLIST'")
    fun observeWatchlistCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM movies WHERE status = 'WATCHLIST'")
    suspend fun countWatchlist(): Int

    @Upsert
    suspend fun upsert(movie: MovieEntry)

    @Query("DELETE FROM movies WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<Playlist>>

    @Query("SELECT COUNT(*) FROM playlists")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM playlist_tracks")
    fun observeAllTracks(): Flow<List<PlaylistTrack>>

    @Insert
    suspend fun insert(playlist: Playlist): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrack(track: PlaylistTrack)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrack(playlistId: Long, trackId: Long)
}

@Database(
    entities = [FavoriteWallpaper::class, AnimeEntry::class, MovieEntry::class, Playlist::class, PlaylistTrack::class],
    version = 1,
    exportSchema = false
)
abstract class GladDatabase : RoomDatabase() {
    abstract fun wallpaperDao(): WallpaperDao
    abstract fun animeDao(): AnimeDao
    abstract fun movieDao(): MovieDao
    abstract fun playlistDao(): PlaylistDao
}
