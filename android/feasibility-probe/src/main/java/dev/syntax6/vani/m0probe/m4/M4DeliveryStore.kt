package dev.syntax6.vani.m0probe.m4

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class StoredBundle(
    val messageId: String,
    val destinationId: String,
    val priority: M4Protocol.Priority,
    val createdAt: Long,
    val expiresAt: Long,
    val hopLimit: Int,
    val copyBudget: Int,
    val attempts: Int,
    val state: M4Protocol.DeliveryState,
    val payload: ByteArray,
)

class M4DeliveryStore(
    context: Context,
    private val maxItems: Int = M4Protocol.MAX_QUEUE_ITEMS,
    private val maxBytes: Int = M4Protocol.MAX_QUEUE_BYTES,
) {
    private val helper = Database(context.applicationContext)

    @Synchronized
    fun enqueue(bundle: StoredBundle) {
        require(bundle.payload.size <= M4Protocol.MAX_BUNDLE_BYTES + 64)
        val db = helper.writableDatabase
        val now = System.currentTimeMillis()
        expire(now, db)
        if (exists(bundle.messageId, db)) return
        enforceCapacity(bundle.payload.size, db)
        val values = ContentValues().apply {
            put("id", bundle.messageId)
            put("destination", bundle.destinationId)
            put("priority", bundle.priority.wire)
            put("created_at", bundle.createdAt)
            put("expires_at", bundle.expiresAt)
            put("hop_limit", bundle.hopLimit)
            put("copy_budget", bundle.copyBudget)
            put("attempts", bundle.attempts)
            put("state", bundle.state.name)
            put("payload", bundle.payload)
        }
        db.insertOrThrow("outbox", null, values)
    }

    @Synchronized
    fun nextEligible(destinationId: String?, now: Long = System.currentTimeMillis()): StoredBundle? {
        val db = helper.writableDatabase
        expire(now, db)
        val where = if (destinationId == null) "state IN ('QUEUED','RELAYED') AND expires_at > ?" else
            "state IN ('QUEUED','RELAYED') AND destination = ? AND expires_at > ?"
        val args = if (destinationId == null) arrayOf(now.toString()) else arrayOf(destinationId, now.toString())
        db.query("outbox", null, where, args, null, null, "priority DESC, expires_at ASC, created_at ASC", "1").use { c ->
            if (!c.moveToFirst()) return null
            return row(c)
        }
    }

    @Synchronized
    fun updateState(messageId: String, state: M4Protocol.DeliveryState, attempts: Int? = null) {
        val values = ContentValues().apply {
            put("state", state.name)
            attempts?.let { put("attempts", it) }
        }
        helper.writableDatabase.update("outbox", values, "id = ?", arrayOf(messageId))
    }

    @Synchronized
    fun remove(messageId: String) {
        helper.writableDatabase.delete("outbox", "id = ?", arrayOf(messageId))
    }

    @Synchronized
    fun markSeen(messageId: String, seenAt: Long = System.currentTimeMillis()): Boolean {
        val db = helper.writableDatabase
        val values = ContentValues().apply { put("id", messageId); put("seen_at", seenAt) }
        return db.insertWithOnConflict("seen", null, values, SQLiteDatabase.CONFLICT_IGNORE) != -1L
    }

    @Synchronized
    fun hasSeen(messageId: String): Boolean =
        helper.readableDatabase.query("seen", arrayOf("id"), "id = ?", arrayOf(messageId), null, null, null, "1").use { it.moveToFirst() }

    @Synchronized
    fun recoverAfterRestart() {
        val values = ContentValues().apply { put("state", M4Protocol.DeliveryState.QUEUED.name) }
        helper.writableDatabase.update(
            "outbox", values, "state IN ('TRANSFERRED','RELAYED') AND expires_at > ?",
            arrayOf(System.currentTimeMillis().toString())
        )
    }

    @Synchronized
    fun purgeExpired(now: Long = System.currentTimeMillis()): Int = expire(now, helper.writableDatabase)

    @Synchronized
    fun saveFragment(fragment: M4Protocol.Fragment, receivedAt: Long = System.currentTimeMillis()) {
        val values = ContentValues().apply {
            put("message_id", fragment.messageId)
            put("idx", fragment.fragmentIndex)
            put("count", fragment.fragmentCount)
            put("total_bytes", fragment.totalBytes)
            put("payload", fragment.payload)
            put("received_at", receivedAt)
        }
        helper.writableDatabase.insertWithOnConflict("fragments", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    @Synchronized
    fun hasFragment(messageId: String, index: Int): Boolean = helper.readableDatabase.query("fragments", arrayOf("idx"), "message_id = ? AND idx = ?", arrayOf(messageId, index.toString()), null, null, null, "1").use { it.moveToFirst() }

    @Synchronized
    fun persistInbox(messageId: String, payload: ByteArray, state: M4Protocol.DeliveryState) {
        val values = ContentValues().apply { put("id", messageId); put("payload", payload); put("state", state.name); put("updated_at", System.currentTimeMillis()) }
        helper.writableDatabase.insertWithOnConflict("inbox", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    @Synchronized
    fun inboxState(messageId: String): M4Protocol.DeliveryState? = helper.readableDatabase.query("inbox", arrayOf("state"), "id = ?", arrayOf(messageId), null, null, null, "1").use { if (!it.moveToFirst()) null else M4Protocol.DeliveryState.valueOf(it.getString(0)) }

    @Synchronized
    fun updateInboxState(messageId: String, state: M4Protocol.DeliveryState) {
        val values = ContentValues().apply { put("state", state.name); put("updated_at", System.currentTimeMillis()) }
        helper.writableDatabase.update("inbox", values, "id = ?", arrayOf(messageId))
    }

    @Synchronized
    fun loadFragments(messageId: String): List<M4Protocol.Fragment> {
        val result = mutableListOf<M4Protocol.Fragment>()
        helper.readableDatabase.query("fragments", null, "message_id = ?", arrayOf(messageId), null, null, "idx ASC").use { c ->
            while (c.moveToNext()) {
                result += M4Protocol.Fragment(
                    c.getString(c.getColumnIndexOrThrow("message_id")),
                    c.getInt(c.getColumnIndexOrThrow("idx")),
                    c.getInt(c.getColumnIndexOrThrow("count")),
                    c.getInt(c.getColumnIndexOrThrow("total_bytes")),
                    c.getBlob(c.getColumnIndexOrThrow("payload"))
                )
            }
        }
        return result
    }

    @Synchronized
    fun clearFragments(messageId: String) {
        helper.writableDatabase.delete("fragments", "message_id = ?", arrayOf(messageId))
    }

    fun close() = helper.close()

    private fun exists(id: String, db: SQLiteDatabase): Boolean =
        db.query("outbox", arrayOf("id"), "id = ?", arrayOf(id), null, null, null, "1").use { it.moveToFirst() }

    private fun enforceCapacity(incomingBytes: Int, db: SQLiteDatabase) {
        while (queueItems(db) >= maxItems || queueBytes(db) + incomingBytes > maxBytes) {
            val deleted = db.delete(
                "outbox",
                "id = (SELECT id FROM outbox WHERE state IN ('QUEUED','RELAYED') ORDER BY priority ASC, expires_at ASC, created_at ASC LIMIT 1)",
                null
            )
            if (deleted == 0) throw IllegalStateException("queue capacity cannot accept bundle")
        }
    }

    private fun queueItems(db: SQLiteDatabase): Int =
        db.rawQuery("SELECT COUNT(*) FROM outbox WHERE state IN ('QUEUED','RELAYED')", null).use { it.moveToFirst(); it.getInt(0) }

    private fun queueBytes(db: SQLiteDatabase): Int =
        db.rawQuery("SELECT COALESCE(SUM(LENGTH(payload)),0) FROM outbox WHERE state IN ('QUEUED','RELAYED')", null).use { it.moveToFirst(); it.getInt(0) }

    private fun expire(now: Long, db: SQLiteDatabase): Int {
        val values = ContentValues().apply { put("state", M4Protocol.DeliveryState.EXPIRED.name) }
        return db.update("outbox", values, "expires_at <= ? AND state NOT IN ('DELIVERED_DEVICE','PLAYBACK_STARTED','ACKNOWLEDGED_PERSON','EXPIRED','FAILED')", arrayOf(now.toString()))
    }

    private fun row(c: android.database.Cursor): StoredBundle = StoredBundle(
        c.getString(c.getColumnIndexOrThrow("id")),
        c.getString(c.getColumnIndexOrThrow("destination")),
        M4Protocol.Priority.values().first { it.wire == c.getInt(c.getColumnIndexOrThrow("priority")) },
        c.getLong(c.getColumnIndexOrThrow("created_at")),
        c.getLong(c.getColumnIndexOrThrow("expires_at")),
        c.getInt(c.getColumnIndexOrThrow("hop_limit")),
        c.getInt(c.getColumnIndexOrThrow("copy_budget")),
        c.getInt(c.getColumnIndexOrThrow("attempts")),
        M4Protocol.DeliveryState.valueOf(c.getString(c.getColumnIndexOrThrow("state"))),
        c.getBlob(c.getColumnIndexOrThrow("payload"))
    )

    private class Database(context: Context) : SQLiteOpenHelper(context, "vani_m4.db", null, 2) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE outbox(id TEXT PRIMARY KEY,destination TEXT NOT NULL,priority INTEGER NOT NULL,created_at INTEGER NOT NULL,expires_at INTEGER NOT NULL,hop_limit INTEGER NOT NULL,copy_budget INTEGER NOT NULL,attempts INTEGER NOT NULL,state TEXT NOT NULL,payload BLOB NOT NULL)")
            db.execSQL("CREATE INDEX outbox_ready ON outbox(state,priority,expires_at)")
            db.execSQL("CREATE TABLE seen(id TEXT PRIMARY KEY,seen_at INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE inbox(id TEXT PRIMARY KEY,payload BLOB NOT NULL,state TEXT NOT NULL,updated_at INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE fragments(message_id TEXT NOT NULL,idx INTEGER NOT NULL,count INTEGER NOT NULL,total_bytes INTEGER NOT NULL,payload BLOB NOT NULL,received_at INTEGER NOT NULL,PRIMARY KEY(message_id,idx))")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 2) db.execSQL("CREATE TABLE IF NOT EXISTS inbox(id TEXT PRIMARY KEY,payload BLOB NOT NULL,state TEXT NOT NULL,updated_at INTEGER NOT NULL)")
        }
    }
}
