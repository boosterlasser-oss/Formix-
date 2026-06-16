package com.fantasyfoodplanner.logic

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Verwaltet das strukturierte Validierungsprotokoll.
 * Pfad: Interner Speicher / system_validation.log
 * Limit: 500 Einträge (FIFO)
 */
object SystemLogger {
    private const val LOG_FILE_NAME = "system_validation.log"
    private const val MAX_ENTRIES = 500

    fun log(context: Context, entry: String) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            
            // FIFO Rotation prüfen
            val lines = if (file.exists()) file.readLines() else emptyList()
            
            // Ein "Eintrag" besteht aus mehreren Zeilen (getrennt durch ---)
            // Wir zählen einfach die Zeilen als grobes Maß oder die Trenner
            val entryCount = lines.count { it.startsWith("---") } / 2
            
            val updatedLines = if (entryCount >= MAX_ENTRIES) {
                // Entferne den ältesten Block (bis zum zweiten Trenner)
                val firstEndIndex = lines.indexOfFirst { it.startsWith("---") }
                val secondEndIndex = lines.indexOfFirst { it.startsWith("---") && lines.indexOf(it) > firstEndIndex }
                if (secondEndIndex != -1) lines.drop(secondEndIndex + 1) else lines.drop(lines.size / 4)
            } else lines

            val out = FileOutputStream(file, false)
            updatedLines.forEach { out.write((it + "\n").toByteArray()) }
            out.write((entry + "\n\n").toByteArray())
            out.flush()
            out.close()
        } catch (e: Exception) {
            Log.e("SystemLogger", "Log failed", e)
            handleCorruption(context)
        }
    }

    private fun handleCorruption(context: Context) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) {
                file.renameTo(File(context.filesDir, "system_validation_${System.currentTimeMillis()}.corrupt"))
            }
        } catch (e: Exception) {
            Log.e("SystemLogger", "Could not rename corrupt file", e)
        }
    }

    fun readLog(context: Context): String {
        return try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) file.readText() else "Keine Log-Einträge vorhanden."
        } catch (e: IOException) {
            "Fehler beim Lesen des Logs."
        }
    }

    fun clearLog(context: Context) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Log.e("SystemLogger", "Clear failed", e)
        }
    }
}
