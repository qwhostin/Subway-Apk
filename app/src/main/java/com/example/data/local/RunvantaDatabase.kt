package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AccessoryType
import com.example.data.model.CharacterAppearance
import com.example.data.model.HairStyle
import com.example.data.model.OutfitStyle
import com.example.data.model.ShoeStyle
import com.example.data.model.TrailEffect
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isDefault: Boolean,
    val unlockCoins: Int,
    val faceImagePath: String?,
    val faceScale: Float,
    val faceOffsetX: Float,
    val faceOffsetY: Float,
    val skinColorRgb: Long,
    val hairStyle: String,
    val hairColorRgb: Long,
    val outfitStyle: String,
    val outfitColorRgb: Long,
    val accentColorRgb: Long,
    val shoeStyle: String,
    val shoeColorRgb: Long,
    val accessory: String,
    val trailEffect: String,
    val createdAt: Long
) {
    fun toModel(): CharacterAppearance = CharacterAppearance(
        id = id,
        name = name,
        isDefault = isDefault,
        unlockCoins = unlockCoins,
        faceImagePath = faceImagePath,
        faceScale = faceScale,
        faceOffsetX = faceOffsetX,
        faceOffsetY = faceOffsetY,
        skinColorRgb = skinColorRgb,
        hairStyle = runCatching { HairStyle.valueOf(hairStyle) }.getOrDefault(HairStyle.SPIKY),
        hairColorRgb = hairColorRgb,
        outfitStyle = runCatching { OutfitStyle.valueOf(outfitStyle) }.getOrDefault(OutfitStyle.RUNNER_SUIT),
        outfitColorRgb = outfitColorRgb,
        accentColorRgb = accentColorRgb,
        shoeStyle = runCatching { ShoeStyle.valueOf(shoeStyle) }.getOrDefault(ShoeStyle.TURBO_SNEAKERS),
        shoeColorRgb = shoeColorRgb,
        accessory = runCatching { AccessoryType.valueOf(accessory) }.getOrDefault(AccessoryType.NONE),
        trailEffect = runCatching { TrailEffect.valueOf(trailEffect) }.getOrDefault(TrailEffect.CYAN_SPARKS),
        createdAt = createdAt
    )

    companion object {
        fun fromModel(model: CharacterAppearance): CharacterEntity = CharacterEntity(
            id = model.id,
            name = model.name,
            isDefault = model.isDefault,
            unlockCoins = model.unlockCoins,
            faceImagePath = model.faceImagePath,
            faceScale = model.faceScale,
            faceOffsetX = model.faceOffsetX,
            faceOffsetY = model.faceOffsetY,
            skinColorRgb = model.skinColorRgb,
            hairStyle = model.hairStyle.name,
            hairColorRgb = model.hairColorRgb,
            outfitStyle = model.outfitStyle.name,
            outfitColorRgb = model.outfitColorRgb,
            accentColorRgb = model.accentColorRgb,
            shoeStyle = model.shoeStyle.name,
            shoeColorRgb = model.shoeColorRgb,
            accessory = model.accessory.name,
            trailEffect = model.trailEffect.name,
            createdAt = model.createdAt
        )
    }
}

@Entity(tableName = "mission_progress")
data class MissionProgressEntity(
    @PrimaryKey val id: String,
    val progress: Int,
    val claimed: Boolean,
    val cycleKey: String
)

@Entity(tableName = "achievement_progress")
data class AchievementProgressEntity(
    @PrimaryKey val id: String,
    val progress: Int,
    val unlocked: Boolean,
    val claimed: Boolean
)

@Entity(tableName = "run_records")
data class RunRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val score: Int,
    val distanceMeters: Int,
    val coinsEarned: Int,
    val obstaclesCleared: Int,
    val durationSec: Int,
    val modeName: String,
    val environmentId: String,
    val usedCustomCharacter: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters ORDER BY isDefault DESC, createdAt DESC")
    fun observeAll(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CharacterEntity?

    @Query("SELECT COUNT(*) FROM characters")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CharacterEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(entities: List<CharacterEntity>)

    @Query("DELETE FROM characters WHERE id = :id AND isDefault = 0")
    suspend fun deleteCustomById(id: String)

    @Query("DELETE FROM characters WHERE isDefault = 0")
    suspend fun deleteAllCustom()
}

@Dao
interface ProgressionDao {
    @Query("SELECT * FROM mission_progress")
    fun observeMissions(): Flow<List<MissionProgressEntity>>

    @Query("SELECT * FROM mission_progress")
    suspend fun getAllMissions(): List<MissionProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMission(entity: MissionProgressEntity)

    @Query("DELETE FROM mission_progress")
    suspend fun clearMissions()

    @Query("SELECT * FROM achievement_progress")
    fun observeAchievements(): Flow<List<AchievementProgressEntity>>

    @Query("SELECT * FROM achievement_progress")
    suspend fun getAllAchievements(): List<AchievementProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievement(entity: AchievementProgressEntity)

    @Query("DELETE FROM achievement_progress")
    suspend fun clearAchievements()

    @Insert
    suspend fun insertRun(record: RunRecordEntity)

    @Query("SELECT * FROM run_records ORDER BY timestamp DESC LIMIT 20")
    fun observeRecentRuns(): Flow<List<RunRecordEntity>>

    @Query("DELETE FROM run_records")
    suspend fun clearRuns()
}

@Database(
    entities = [
        CharacterEntity::class,
        MissionProgressEntity::class,
        AchievementProgressEntity::class,
        RunRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RunvantaDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun progressionDao(): ProgressionDao

    companion object {
        @Volatile
        private var INSTANCE: RunvantaDatabase? = null

        fun getInstance(context: Context): RunvantaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RunvantaDatabase::class.java,
                    "runvanta_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
