package com.relentnet.cuttracker.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DayLogDao {
    @Query("SELECT * FROM DayLog WHERE date = :date") fun observe(date: LocalDate): Flow<DayLog?>
    @Query("SELECT * FROM DayLog WHERE date = :date") suspend fun get(date: LocalDate): DayLog?
    @Query("SELECT * FROM DayLog ORDER BY date") fun observeAll(): Flow<List<DayLog>>
    @Query("SELECT * FROM DayLog ORDER BY date") suspend fun all(): List<DayLog>
    @Upsert suspend fun upsert(log: DayLog)
    @Query("DELETE FROM DayLog") suspend fun clear()
}

@Dao
interface WeighInDao {
    @Query("SELECT * FROM WeighIn ORDER BY date") fun observeAll(): Flow<List<WeighIn>>
    @Query("SELECT * FROM WeighIn ORDER BY date") suspend fun all(): List<WeighIn>
    @Query("SELECT * FROM WeighIn WHERE date = :date") suspend fun get(date: LocalDate): WeighIn?
    @Upsert suspend fun upsert(w: WeighIn)
    @Query("DELETE FROM WeighIn WHERE date = :date") suspend fun delete(date: LocalDate)
    @Query("DELETE FROM WeighIn") suspend fun clear()
}

@Dao
interface LiftSetDao {
    @Query("SELECT * FROM LiftSet ORDER BY date, setIndex") fun observeAll(): Flow<List<LiftSet>>
    @Query("SELECT * FROM LiftSet ORDER BY date, setIndex") suspend fun all(): List<LiftSet>
    @Insert suspend fun insertAll(sets: List<LiftSet>)
    @Query("DELETE FROM LiftSet WHERE date = :date AND exerciseId = :exerciseId") suspend fun deleteFor(date: LocalDate, exerciseId: String)
    @Query("DELETE FROM LiftSet") suspend fun clear()
}

@Dao
interface ProgressPhotoDao {
    @Query("SELECT * FROM ProgressPhoto ORDER BY date") fun observeAll(): Flow<List<ProgressPhoto>>
    @Query("SELECT * FROM ProgressPhoto ORDER BY date") suspend fun all(): List<ProgressPhoto>
    @Upsert suspend fun upsert(p: ProgressPhoto)
    @Query("DELETE FROM ProgressPhoto WHERE date = :date") suspend fun delete(date: LocalDate)
    @Query("DELETE FROM ProgressPhoto") suspend fun clear()
}

@Dao
interface XpEventDao {
    @Query("SELECT * FROM XpEvent ORDER BY at") fun observeAll(): Flow<List<XpEvent>>
    @Query("SELECT * FROM XpEvent ORDER BY at") suspend fun all(): List<XpEvent>
    @Query("SELECT * FROM XpEvent WHERE date = :date") suspend fun forDate(date: LocalDate): List<XpEvent>
    @Query("SELECT COALESCE(SUM(amount), 0) FROM XpEvent") fun observeTotal(): Flow<Int>
    @Query("SELECT COALESCE(SUM(amount), 0) FROM XpEvent") suspend fun total(): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(e: XpEvent): Long
    @Insert suspend fun insertAll(e: List<XpEvent>)
    @Query("DELETE FROM XpEvent WHERE id IN (:ids)") suspend fun deleteIds(ids: List<Long>)
    @Query("DELETE FROM XpEvent WHERE date = :date AND kind = :kind AND (:ref IS NULL OR ref = :ref)") suspend fun delete(date: LocalDate, kind: String, ref: String?)
    @Query("SELECT COUNT(*) FROM XpEvent WHERE kind = :kind AND ref = :ref") suspend fun count(kind: String, ref: String): Int
    @Query("DELETE FROM XpEvent") suspend fun clear()
}

@Dao
interface BadgeDao {
    @Query("SELECT * FROM Badge") fun observeAll(): Flow<List<Badge>>
    @Query("SELECT * FROM Badge") suspend fun all(): List<Badge>
    @Query("SELECT * FROM Badge WHERE id = :id") suspend fun get(id: String): Badge?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(b: Badge): Long
    @Query("DELETE FROM Badge") suspend fun clear()
}

@Dao
interface WeeklyProjectDao {
    @Query("SELECT * FROM WeeklyProject ORDER BY weekStart") fun observeAll(): Flow<List<WeeklyProject>>
    @Query("SELECT * FROM WeeklyProject ORDER BY weekStart") suspend fun all(): List<WeeklyProject>
    @Query("SELECT * FROM WeeklyProject WHERE weekStart = :weekStart") suspend fun get(weekStart: LocalDate): WeeklyProject?
    @Query("SELECT * FROM WeeklyProject WHERE weekStart < :weekStart ORDER BY weekStart DESC LIMIT 1") suspend fun previous(weekStart: LocalDate): WeeklyProject?
    @Upsert suspend fun upsert(p: WeeklyProject)
    @Query("DELETE FROM WeeklyProject") suspend fun clear()
}

@Database(
    entities = [DayLog::class, WeighIn::class, LiftSet::class, ProgressPhoto::class, XpEvent::class, Badge::class, WeeklyProject::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class CutDb : RoomDatabase() {
    abstract fun dayLogs(): DayLogDao
    abstract fun weighIns(): WeighInDao
    abstract fun liftSets(): LiftSetDao
    abstract fun photos(): ProgressPhotoDao
    abstract fun xp(): XpEventDao
    abstract fun badges(): BadgeDao
    abstract fun projects(): WeeklyProjectDao

    companion object {
        const val NAME = "cut.db"

        /** Migrations are registered here from v1 on. Never fallbackToDestructiveMigration. */
        fun build(context: Context): CutDb = Room.databaseBuilder(context, CutDb::class.java, NAME)
            .addMigrations(*Migrations.all)
            .build()
    }
}

/**
 * Add one Migration per schema bump and bump the @Database version; the exported schema under app/schemas is the
 * reference. Template:
 *
 *     val M1_2 = object : Migration(1, 2) {
 *         override fun migrate(db: SQLiteConnection) { db.execSQL("ALTER TABLE DayLog ADD COLUMN note TEXT") }
 *     }
 */
object Migrations {
    val all = arrayOf<androidx.room.migration.Migration>()
}
