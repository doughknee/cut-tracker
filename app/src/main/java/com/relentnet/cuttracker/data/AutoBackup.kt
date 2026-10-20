package com.relentnet.cuttracker.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * A weekly copy of everything but photos, written into a folder the user picked with the system picker (on the phone,
 * a USB drive, or a synced folder another app manages). No network: the app only writes a file where it was allowed to.
 */
object AutoBackup {
    const val EVERY_DAYS = 7L

    /** "Documents/Sendline" from a tree URI, for the Data page. */
    fun folderName(tree: String): String = runCatching {
        DocumentsContract.getTreeDocumentId(Uri.parse(tree)).substringAfter(':').ifBlank { "the folder you picked" }
    }.getOrDefault("the folder you picked")

    /**
     * Writes sendline-<date>.json into the picked folder when the last export is [EVERY_DAYS] old or [force] is set.
     * Returns true when a file was written. A revoked folder or a full disk fails quietly; the Data page shows the
     * date of the last copy that did land.
     */
    suspend fun runIfDue(context: Context, repo: Repo, force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val s = repo.settings.current()
        val tree = s.autoBackupFolder?.let(Uri::parse) ?: return@withContext false
        val today = LocalDate.now()
        val last = s.lastExport
        if (!force && last != null && ChronoUnit.DAYS.between(last, today) < EVERY_DAYS) return@withContext false
        runCatching {
            val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            val doc = DocumentsContract.createDocument(context.contentResolver, parent, "application/json", "sendline-$today.json")
                ?: error("the folder refused the file")
            val text = backupJson.encodeToString(Backup.serializer(), repo.buildBackup())
            context.contentResolver.openOutputStream(doc)!!.use { it.write(text.toByteArray()) }
            repo.updateSettings { it.copy(lastExport = today) }
        }.isSuccess
    }
}
