package com.relentnet.cuttracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

@Entity
data class DayLog(
    /** Program day, after the rollover hour. */
    @PrimaryKey val date: LocalDate,
    val mode: DayMode? = null,
    val doneItemIds: Set<String> = emptySet(),
    /** v1 dinner pick; migrated into [picks], kept so the column stays valid. */
    val dinnerIndex: Int? = null,
    /** Slot id to what was logged: "lib:<id>" or "custom:<protein>:<calories>:<name>". Empty means the slot's default. */
    @ColumnInfo(defaultValue = "") val picks: Map<String, String> = emptyMap(),
)

@Entity
data class WeighIn(@PrimaryKey val date: LocalDate, val lb: Double)

@Entity
data class LiftSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val exerciseId: String,
    /** 0 for bodyweight. */
    val weightLb: Double,
    val reps: Int,
    val setIndex: Int,
)

@Entity
data class ProgressPhoto(@PrimaryKey val date: LocalDate, val path: String)

@Entity
data class XpEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val at: Instant,
    val kind: String,
    val amount: Int,
    val ref: String? = null,
)

@Entity
data class Badge(@PrimaryKey val id: String, val earnedAt: Instant)

@Entity
data class WeeklyProject(
    @PrimaryKey val weekStart: LocalDate,
    val id: String,
    val progress: Int = 0,
    val done: Boolean = false,
)

class Converters {
    @TypeConverter fun dateToString(d: LocalDate?): String? = d?.toString()
    @TypeConverter fun stringToDate(s: String?): LocalDate? = s?.let(LocalDate::parse)
    @TypeConverter fun instantToLong(i: Instant?): Long? = i?.toEpochMilli()
    @TypeConverter fun longToInstant(l: Long?): Instant? = l?.let(Instant::ofEpochMilli)
    @TypeConverter fun setToString(s: Set<String>?): String? = s?.joinToString(",")
    @TypeConverter fun stringToSet(s: String?): Set<String>? = s?.split(',')?.filter { it.isNotEmpty() }?.toSet()
    @TypeConverter fun mapToString(m: Map<String, String>?): String? = m?.entries?.joinToString(";") { it.key + "=" + it.value }
    @TypeConverter fun stringToMap(s: String?): Map<String, String>? =
        s?.split(';')?.filter { '=' in it }?.associate { val i = it.indexOf('='); it.substring(0, i) to it.substring(i + 1) }
}
