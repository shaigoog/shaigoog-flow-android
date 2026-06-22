package com.shaigoog.flow.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

data class TradeOperation(
    val id: String = UUID.randomUUID().toString(),
    val kind: String,
    val partyName: String,
    val productName: String,
    val weightGrams: Long,
    val currency: String,
    val amountMinor: Long,
    val status: String = "OPEN",
    val createdAt: Long = System.currentTimeMillis()
)

class OperationDatabase(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE operations (
                id TEXT PRIMARY KEY,
                kind TEXT NOT NULL,
                party_name TEXT NOT NULL,
                product_name TEXT NOT NULL,
                weight_grams INTEGER NOT NULL,
                currency TEXT NOT NULL,
                amount_minor INTEGER NOT NULL,
                status TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun insert(operation: TradeOperation) {
        writableDatabase.insertOrThrow(
            "operations",
            null,
            ContentValues().apply {
                put("id", operation.id)
                put("kind", operation.kind)
                put("party_name", operation.partyName)
                put("product_name", operation.productName)
                put("weight_grams", operation.weightGrams)
                put("currency", operation.currency)
                put("amount_minor", operation.amountMinor)
                put("status", operation.status)
                put("created_at", operation.createdAt)
            }
        )
    }

    fun list(): List<TradeOperation> {
        val result = mutableListOf<TradeOperation>()
        readableDatabase.query(
            "operations",
            null,
            null,
            null,
            null,
            null,
            "created_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += TradeOperation(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    kind = cursor.getString(cursor.getColumnIndexOrThrow("kind")),
                    partyName = cursor.getString(cursor.getColumnIndexOrThrow("party_name")),
                    productName = cursor.getString(cursor.getColumnIndexOrThrow("product_name")),
                    weightGrams = cursor.getLong(cursor.getColumnIndexOrThrow("weight_grams")),
                    currency = cursor.getString(cursor.getColumnIndexOrThrow("currency")),
                    amountMinor = cursor.getLong(cursor.getColumnIndexOrThrow("amount_minor")),
                    status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                )
            }
        }
        return result
    }

    fun updateStatus(id: String, status: String) {
        writableDatabase.update(
            "operations",
            ContentValues().apply { put("status", status) },
            "id = ?",
            arrayOf(id)
        )
    }

    fun delete(id: String) {
        writableDatabase.delete("operations", "id = ?", arrayOf(id))
    }

    companion object {
        private const val DATABASE_NAME = "shaigoog_flow.db"
        private const val DATABASE_VERSION = 1
    }
}
