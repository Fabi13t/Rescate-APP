package com.rescate.app.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper




class RescateDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, "rescate.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE favorites (pack_id INTEGER PRIMARY KEY, saved_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE feed_cache (id INTEGER PRIMARY KEY CHECK (id = 1), json TEXT NOT NULL, updated_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE districts (name TEXT PRIMARY KEY)")
        listOf(
            "Los Olivos", "San Martín de Porres", "Independencia", "Comas", "Miraflores",
            "San Isidro", "Surco", "Jesús María", "Lince", "Pueblo Libre", "San Miguel"
        ).forEach { db.execSQL("INSERT INTO districts(name) VALUES (?)", arrayOf(it)) }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS favorites")
        db.execSQL("DROP TABLE IF EXISTS feed_cache")
        db.execSQL("DROP TABLE IF EXISTS districts")
        onCreate(db)
    }

     
    fun isFavorite(packId: Long): Boolean =
        readableDatabase.rawQuery("SELECT 1 FROM favorites WHERE pack_id = ?", arrayOf(packId.toString()))
            .use { it.moveToFirst() }

    fun setFavorite(packId: Long, favorite: Boolean) {
        if (favorite) {
            val values = ContentValues().apply {
                put("pack_id", packId)
                put("saved_at", System.currentTimeMillis())
            }
            writableDatabase.insertWithOnConflict("favorites", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } else {
            writableDatabase.delete("favorites", "pack_id = ?", arrayOf(packId.toString()))
        }
    }

    fun favoriteIds(): Set<Long> {
        val ids = mutableSetOf<Long>()
        readableDatabase.rawQuery("SELECT pack_id FROM favorites", null).use { c ->
            while (c.moveToNext()) ids.add(c.getLong(0))
        }
        return ids
    }

     
    fun saveFeed(json: String) {
        val values = ContentValues().apply {
            put("id", 1)
            put("json", json)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("feed_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun loadFeed(): String? =
        readableDatabase.rawQuery("SELECT json FROM feed_cache WHERE id = 1", null)
            .use { if (it.moveToFirst()) it.getString(0) else null }

     
    fun districts(): List<String> {
        val list = mutableListOf<String>()
        readableDatabase.rawQuery("SELECT name FROM districts ORDER BY name", null).use { c ->
            while (c.moveToNext()) list.add(c.getString(0))
        }
        return list
    }

    companion object {
        @Volatile
        private var instance: RescateDbHelper? = null

        fun get(context: Context): RescateDbHelper =
            instance ?: synchronized(this) {
                instance ?: RescateDbHelper(context.applicationContext).also { instance = it }
            }
    }
}
