package com.augt.localseek.indexing

import java.io.File

object DocumentParser {

    data class ParsedDocument(
        val title: String,
        val body: String,
        val fileType: String
    )

    private val supportedExtensions = setOf("txt", "md", "csv", "json", "kt", "py", "java")

    fun canParse(file: File): Boolean =
        file.extension.lowercase() in supportedExtensions

    fun parse(file: File): ParsedDocument? {
        if (!file.exists() || !file.canRead()) return null
        val ext = file.extension.lowercase()
        if (ext !in supportedExtensions) return null

        return try {
            val body = file.readText(Charsets.UTF_8)
                .take(50_000) // cap at 50KB
                .trim()

            if (body.isEmpty()) return null

            ParsedDocument(
                title = file.nameWithoutExtension,
                body = body,
                fileType = ext
            )
        } catch (e: Exception) {
            null
        }
    }
}