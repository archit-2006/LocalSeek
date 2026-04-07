package com.augt.localseek.indexing

import android.content.Context
import android.os.Environment
import com.augt.localseek.data.AppDatabase
import com.augt.localseek.data.DocumentEntity
import java.io.File

class FileIndexer(context: Context) {

    private val dao = AppDatabase.getInstance(context).documentDao()

    // Directories to scan. These are the standard public directories on Android.
    private val scanRoots: List<File> get() = listOf(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    ).filter { it.exists() && it.isDirectory }

    // This data class helps us track metrics for our UI and research paper
    data class IndexStats(
        val newFiles: Int = 0,
        val updatedFiles: Int = 0,
        val skippedFiles: Int = 0,
        val errors: Int = 0
    )

    /**
     * Walks all specified directories, parses supported files, and inserts them into the DB.
     * This is a heavy operation, so it is a 'suspend' function that must run on a background thread.
     */
    suspend fun runFullIndex(): IndexStats {
        var newCount = 0
        var updatedCount = 0
        var skippedCount = 0
        var errorCount = 0

        // Find all parseable files in our target directories
        val allFiles = scanRoots.flatMap { root ->
            root.walkTopDown()
                .filter { it.isFile && DocumentParser.canParse(it) }
                .toList()
        }

        for (file in allFiles) {
            try {
                // Check if we already indexed this exact version of the file
                val existingModifiedAt = dao.getModifiedAt(file.absolutePath)

                if (existingModifiedAt != null && existingModifiedAt == file.lastModified()) {
                    skippedCount++
                    continue // Skip parsing, file hasn't changed
                }

                // Extract the text
                val parsed = DocumentParser.parse(file)
                if (parsed == null) {
                    errorCount++
                    continue
                }

                // Insert into SQLite (Room will automatically update the FTS5 table)
                dao.insert(DocumentEntity(
                    filePath = file.absolutePath,
                    title = parsed.title,
                    body = parsed.body,
                    fileType = parsed.fileType,
                    modifiedAt = file.lastModified(),
                    sizeBytes = file.length()
                ))

                if (existingModifiedAt == null) newCount++ else updatedCount++

            } catch (e: Exception) {
                errorCount++
            }
        }

        return IndexStats(newCount, updatedCount, skippedCount, errorCount)
    }
}