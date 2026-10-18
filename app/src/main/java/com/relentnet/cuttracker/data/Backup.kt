package com.relentnet.cuttracker.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

/** Plain-string DTOs so the export survives entity changes. Version bumps when the shape changes. */
@Serializable
data class Backup(
    val version: Int = 1,
    val exportedAt: String,
    val settings: SettingsDto,
    val dayLogs: List<DayLogDto>,
    val weighIns: List<WeighInDto>,
    val liftSets: List<LiftSetDto>,
    val photos: List<PhotoDto>,
    val xpEvents: List<XpEventDto>,
    val badges: List<BadgeDto>,
    val projects: List<ProjectDto>,
)

@Serializable
data class SettingsDto(
    val onboarded: Boolean, val phase: Int, val startDate: String, val lengthWeeks: Int, val startWeight: Double, val targetWeight: Double,
    val caloriesP1: Int, val caloriesP2: Int, val protein: Int, val meals: List<Meal>, val exercises: List<Exercise>, val checkpoints: List<Checkpoint>,
    val weighInTime: String, val pickTime: String, val resetHour: Int, val haptics: Boolean, val theme: String,
    val lastOpenDate: String? = null, val welcomeBackSeen: String? = null, val workoutsPerWeek: Int = 3,
)

@Serializable data class DayLogDto(val date: String, val mode: String?, val done: List<String>, val dinnerIndex: Int?)
@Serializable data class WeighInDto(val date: String, val lb: Double)
@Serializable data class LiftSetDto(val date: String, val exerciseId: String, val weightLb: Double, val reps: Int, val setIndex: Int)
@Serializable data class PhotoDto(val date: String, val file: String)
@Serializable data class XpEventDto(val date: String, val at: Long, val kind: String, val amount: Int, val ref: String?)
@Serializable data class BadgeDto(val id: String, val earnedAt: Long)
@Serializable data class ProjectDto(val weekStart: String, val id: String, val progress: Int, val done: Boolean)

val backupJson = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

fun Settings.toDto() = SettingsDto(
    onboarded, phase, startDate.toString(), lengthWeeks, startWeight, targetWeight, caloriesP1, caloriesP2, protein, meals, exercises, checkpoints,
    weighInTime.toString(), pickTime.toString(), resetHour, haptics, theme, lastOpenDate?.toString(), welcomeBackSeen?.toString(), workoutsPerWeek,
)

fun SettingsDto.toSettings() = Settings(
    onboarded, phase, LocalDate.parse(startDate), lengthWeeks, startWeight, targetWeight, caloriesP1, caloriesP2, protein, workoutsPerWeek, meals, exercises, checkpoints,
    java.time.LocalTime.parse(weighInTime), java.time.LocalTime.parse(pickTime), resetHour, haptics, theme,
    lastOpenDate?.let(LocalDate::parse), welcomeBackSeen?.let(LocalDate::parse),
)

suspend fun Repo.buildBackup(): Backup = Backup(
    exportedAt = Instant.now().toString(),
    settings = settings.current().toDto(),
    dayLogs = db.dayLogs().all().map { DayLogDto(it.date.toString(), it.mode?.name, it.doneItemIds.toList(), it.dinnerIndex) },
    weighIns = db.weighIns().all().map { WeighInDto(it.date.toString(), it.lb) },
    liftSets = db.liftSets().all().map { LiftSetDto(it.date.toString(), it.exerciseId, it.weightLb, it.reps, it.setIndex) },
    photos = db.photos().all().map { PhotoDto(it.date.toString(), java.io.File(it.path).name) },
    xpEvents = db.xp().all().map { XpEventDto(it.date.toString(), it.at.toEpochMilli(), it.kind, it.amount, it.ref) },
    badges = db.badges().all().map { BadgeDto(it.id, it.earnedAt.toEpochMilli()) },
    projects = db.projects().all().map { ProjectDto(it.weekStart.toString(), it.id, it.progress, it.done) },
)

/** backup.json plus every photo, as a zip. */
suspend fun Repo.writeZip(out: java.io.File) {
    val json = backupJson.encodeToString(Backup.serializer(), buildBackup())
    java.util.zip.ZipOutputStream(out.outputStream().buffered()).use { zip ->
        zip.putNextEntry(java.util.zip.ZipEntry("backup.json")); zip.write(json.toByteArray()); zip.closeEntry()
        for (f in photosDir.listFiles().orEmpty()) {
            zip.putNextEntry(java.util.zip.ZipEntry("photos/" + f.name)); f.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
        }
    }
}

/** Reads a JSON or zip export. Zip photos land in the photos folder before the rows are restored. */
suspend fun Repo.restoreFrom(input: java.io.InputStream) {
    val bytes = input.readBytes()
    if (bytes.size > 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) {
        var json: String? = null
        java.util.zip.ZipInputStream(bytes.inputStream()).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                when {
                    e.name == "backup.json" -> json = zip.readBytes().decodeToString()
                    e.name.startsWith("photos/") && !e.isDirectory -> photosDir.resolve(java.io.File(e.name).name).outputStream().use { zip.copyTo(it) }
                }
                e = zip.nextEntry
            }
        }
        restoreBackup(backupJson.decodeFromString(Backup.serializer(), json ?: error("no backup.json in zip")))
    } else {
        restoreBackup(backupJson.decodeFromString(Backup.serializer(), bytes.decodeToString()))
    }
}

/** Replaces everything with [b]. Photo rows are kept only when the file exists in the photos folder (restored from a zip or still on device). */
suspend fun Repo.restoreBackup(b: Backup) {
    db.dayLogs().clear(); db.weighIns().clear(); db.liftSets().clear(); db.photos().clear()
    db.xp().clear(); db.badges().clear(); db.projects().clear()
    for (d in b.dayLogs) db.dayLogs().upsert(DayLog(LocalDate.parse(d.date), d.mode?.let { DayMode.valueOf(it) }, d.done.toSet(), d.dinnerIndex))
    for (w in b.weighIns) db.weighIns().upsert(WeighIn(LocalDate.parse(w.date), w.lb))
    db.liftSets().insertAll(b.liftSets.map { LiftSet(date = LocalDate.parse(it.date), exerciseId = it.exerciseId, weightLb = it.weightLb, reps = it.reps, setIndex = it.setIndex) })
    for (p in b.photos) {
        val f = photosDir.resolve(p.file)
        if (f.exists()) db.photos().upsert(ProgressPhoto(LocalDate.parse(p.date), f.absolutePath))
    }
    db.xp().insertAll(b.xpEvents.map { XpEvent(date = LocalDate.parse(it.date), at = Instant.ofEpochMilli(it.at), kind = it.kind, amount = it.amount, ref = it.ref) })
    for (bd in b.badges) db.badges().insert(Badge(bd.id, Instant.ofEpochMilli(bd.earnedAt)))
    for (p in b.projects) db.projects().upsert(WeeklyProject(LocalDate.parse(p.weekStart), p.id, p.progress, p.done))
    settings.save(b.settings.toSettings())
    refreshToday()
}
