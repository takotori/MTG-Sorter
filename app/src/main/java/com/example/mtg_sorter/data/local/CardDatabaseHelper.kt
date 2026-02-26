package com.example.mtg_sorter.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.mtg_sorter.data.model.ScryfallCard
import java.io.File
import java.io.FileOutputStream

class CardDatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "AllPrintings.sqlite"
        private const val DATABASE_VERSION = 1
    }

    private fun copyDatabaseIfMissing() {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        if (!dbFile.exists()) {
            dbFile.parentFile?.mkdirs()
            val tempFile = File(dbFile.absolutePath + ".tmp")
            try {
                context.assets.open(DATABASE_NAME).use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile.renameTo(dbFile)
            } catch (e: Exception) {
                if (tempFile.exists()) tempFile.delete()
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {}
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

    fun getCardByName(name: String): ScryfallCard? {
        // Only try to copy if missing; this might still be slow on first run
        // but it's called from searchCard which is in viewModelScope (background).
        copyDatabaseIfMissing()
        
        val db = try {
            readableDatabase
        } catch (e: Exception) {
            return null
        }
        
        val query = """
            SELECT c.name, c.uuid, s.name as set_name, c.setCode, c.rarity, c.manaCost, c.type, c.text
            FROM cards c
            JOIN sets s ON c.setCode = s.code
            WHERE c.name LIKE ?
            LIMIT 1
        """.trimIndent()
        
        return try {
            db.rawQuery(query, arrayOf("%$name%")).use { cursor ->
                if (cursor.moveToFirst()) {
                    ScryfallCard(
                        id = cursor.getString(1),
                        name = cursor.getString(0),
                        imageUris = null,
                        manaCost = cursor.getString(5),
                        typeLine = cursor.getString(6),
                        oracleText = cursor.getString(7),
                        set = cursor.getString(3),
                        setName = cursor.getString(2),
                        rarity = cursor.getString(4)
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
