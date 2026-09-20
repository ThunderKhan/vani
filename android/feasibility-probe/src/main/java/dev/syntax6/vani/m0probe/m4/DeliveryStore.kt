package dev.syntax6.vani.m4

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

data class DeliveryRecord(
    val messageId: UUID,
    val direction: Direction,
    val state: DeliveryState,
    val bundle: ByteArray,
    val sourceId: String,
    val destination: String,
    val priority: M4Priority,
    val expiresAtEpochMillis: Long,
    val hopLimit: Int,
    val copyBudget: Int,
    val retries: Int,
    val updatedAtEpochMillis: Long,
)

data class QueueLimits(
    val maxMessages: Int = 256,
    val maxBytes: Long = 2L * 1024L * 1024L,
    val perOriginMaxMessages: Int = 64,
    val terminalRetentionMillis: Long = 7L * 24L * 60L * 60L * 1000L,
)

class M4DeliveryStore(
    context: Context,
    private val limits: QueueLimits = QueueLimits(),
) : SQLiteOpenHelper(context, "vani_m4_delivery.db", null, 1), SeenMessageStore {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE records (id TEXT PRIMARY KEY, direction TEXT NOT NULL, state TEXT NOT NULL, bundle BLOB NOT NULL, source_id TEXT NOT NULL, destination TEXT NOT NULL, priority INTEGER NOT NULL, expires_at INTEGER NOT NULL, hop_limit INTEGER NOT NULL, copy_budget INTEGER NOT NULL, retries INTEGER NOT NULL, updated_at INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX records_queue_idx ON records(direction, state, priority DESC, updated_at ASC)")
        db.execSQL("CREATE TABLE seen_messages (id TEXT PRIMARY KEY, expires_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE fragments (message_id TEXT NOT NULL, fragment_index INTEGER NOT NULL, fragment_count INTEGER NOT NULL, total_length INTEGER NOT NULL, payload BLOB NOT NULL, updated_at INTEGER NOT NULL, PRIMARY KEY(message_id, fragment_index))")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    private val db: SQLiteDatabase get() = writableDatabase

    @Synchronized
    fun enqueue(bundle: ByteArray, message: SemanticBundle, direction: Direction = Direction.OUTBOX, now: Long = System.currentTimeMillis()) {
        message.validate(now)
        require(bundle.size <= M4_MAX_BUNDLE_BYTES) { "bundle exceeds store bound" }
        db.transaction {
            cleanupLocked(now)
            val count = Database.count(this)
            val bytes = Database.bytes(this)
            val origin = Database.originCount(this, message.sourceId)
            require(origin < limits.perOriginMaxMessages) { "per-origin queue limit" }
            while (count() >= limits.maxMessages || bytes() + bundle.size > limits.maxBytes) {
                if (!evictLowestPriorityLocked()) throw IllegalStateException("queue limits prevent enqueue")
            }
            val values = ContentValues().apply {
                put("id", message.messageId.toString())
                put("direction", direction.name)
                put("state", DeliveryState.QUEUED.name)
                put("bundle", bundle)
                put("source_id", message.sourceId)
                put("destination", message.destination)
                put("priority", message.priority.wire)
                put("expires_at", message.expiresAtEpochMillis)
                put("hop_limit", message.hopLimit)
                put("copy_budget", message.copyBudget)
                put("retries", 0)
                put("updated_at", now)
            }
            require(insertWithOnConflict("records", null, values, SQLiteDatabase.CONFLICT_IGNORE) != -1L) { "message already exists" }
        }
    }

    @Synchronized
    fun persistReceived(bundle: ByteArray, message: SemanticBundle, now: Long = System.currentTimeMillis()) {
        message.validate(now)
        require(bundle.size <= M4_MAX_BUNDLE_BYTES) { "bundle exceeds store bound" }
        db.transaction {
            cleanupLocked(now)
            val values = ContentValues().apply {
                put("id", message.messageId.toString())
                put("direction", Direction.INBOX.name)
                put("state", DeliveryState.DELIVERED_DEVICE.name)
                put("bundle", bundle)
                put("source_id", message.sourceId)
                put("destination", message.destination)
                put("priority", message.priority.wire)
                put("expires_at", message.expiresAtEpochMillis)
                put("hop_limit", message.hopLimit)
                put("copy_budget", message.copyBudget)
                put("retries", 0)
                put("updated_at", now)
            }
            insertWithOnConflict("records", null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    @Synchronized
    fun transition(id: UUID, next: DeliveryState, now: Long = System.currentTimeMillis()) {
        db.transaction {
            val current = stateLocked(id) ?: throw IllegalArgumentException("unknown message")
            require(allowed(current, next)) { "invalid delivery transition $current -> $next" }
            val values = ContentValues().apply { put("state", next.name); put("updated_at", now) }
            update("records", values, "id=?", arrayOf(id.toString()))
        }
    }

    @Synchronized
    fun markRelayed(id: UUID, remainingHopLimit: Int, remainingCopyBudget: Int, now: Long = System.currentTimeMillis()) {
        require(remainingHopLimit in 0..M4_MAX_HOPS)
        require(remainingCopyBudget in 0..M4_MAX_COPY_BUDGET)
        db.transaction {
            val current = stateLocked(id) ?: throw IllegalArgumentException("unknown message")
            require(current == DeliveryState.QUEUED || current == DeliveryState.TRANSFERRED || current == DeliveryState.RELAYED) { "cannot relay from $current" }
            val values = ContentValues().apply {
                put("state", DeliveryState.RELAYED.name)
                put("hop_limit", remainingHopLimit)
                put("copy_budget", remainingCopyBudget)
                put("updated_at", now)
            }
            update("records", values, "id=?", arrayOf(id.toString()))
        }
    }

    @Synchronized fun state(id: UUID): DeliveryState? =
        db.rawQuery("SELECT state FROM records WHERE id=?", arrayOf(id.toString())).use {
            if (it.moveToFirst()) DeliveryState.valueOf(it.getString(0)) else null
        }

    @Synchronized fun get(id: UUID): DeliveryRecord? =
        db.rawQuery("SELECT id,direction,state,bundle,source_id,destination,priority,expires_at,hop_limit,copy_budget,retries,updated_at FROM records WHERE id=?", arrayOf(id.toString())).use {
            if (!it.moveToFirst()) null else record(it)
        }

    @Synchronized override fun markIfNew(id: UUID, expiresAt: Long, now: Long): Boolean =
        markSeenIfNew(id, expiresAt, now)

    @Synchronized
    fun markSeenIfNew(id: UUID, expiresAt: Long, now: Long = System.currentTimeMillis()): Boolean {
        db.transaction {
            cleanupLocked(now)
            val values = ContentValues().apply { put("id", id.toString()); put("expires_at", expiresAt) }
            return insertWithOnConflict("seen_messages", null, values, SQLiteDatabase.CONFLICT_IGNORE) != -1L
        }
    }

    @Synchronized
    fun saveFragment(frame: M4Frame, now: Long = System.currentTimeMillis()) {
        require(frame.totalLength <= M4_MAX_BUNDLE_BYTES)
        db.transaction {
            val values = ContentValues().apply {
                put("message_id", frame.messageId.toString())
                put("fragment_index", frame.fragmentIndex)
                put("fragment_count", frame.fragmentCount)
                put("total_length", frame.totalLength)
                put("payload", frame.payload)
                put("updated_at", now)
            }
            insertWithOnConflict("fragments", null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    @Synchronized
    fun loadFragments(id: UUID): List<M4Frame> =
        db.rawQuery("SELECT fragment_index,fragment_count,total_length,payload FROM fragments WHERE message_id=? ORDER BY fragment_index", arrayOf(id.toString())).use {
            buildList {
                while (it.moveToNext()) add(M4Frame(id, it.getInt(0), it.getInt(1), it.getInt(2), it.getBlob(3)))
            }
        }

    @Synchronized fun clearFragments(id: UUID) {
        db.delete("fragments", "message_id=?", arrayOf(id.toString()))
    }

    @Synchronized fun expire(now: Long = System.currentTimeMillis()): Int =
        db.transaction { cleanupLocked(now) }

    @Synchronized
    fun pending(direction: Direction = Direction.OUTBOX, now: Long = System.currentTimeMillis()): List<DeliveryRecord> {
        expire(now)
        return db.rawQuery("SELECT id,direction,state,bundle,source_id,destination,priority,expires_at,hop_limit,copy_budget,retries,updated_at FROM records WHERE direction=? AND state IN ('QUEUED','RELAYED','TRANSFERRED') ORDER BY priority DESC, updated_at ASC", arrayOf(direction.name)).use {
            buildList { while (it.moveToNext()) add(record(it)) }
        }
    }

    @Synchronized
    fun incrementRetry(id: UUID, now: Long = System.currentTimeMillis()): Boolean =
        db.transaction {
            val current = get(id) ?: return@transaction false
            if (current.retries >= M4_MAX_RETRIES) return@transaction false
            val values = ContentValues().apply { put("retries", current.retries + 1); put("updated_at", now) }
            update("records", values, "id=?", arrayOf(id.toString())) == 1
        }

    private fun cleanupLocked(now: Long): Int {
        val values = ContentValues().apply {
            put("state", DeliveryState.EXPIRED.name)
            put("updated_at", now)
        }
        val expired = db.update("records", values, "expires_at<=? AND state IN ('QUEUED','TRANSFERRED','RELAYED','CREATED','VALIDATED')", arrayOf(now.toString()))
        db.delete("seen_messages", "expires_at<=?", arrayOf(now.toString()))
        db.delete("records", "state IN ('EXPIRED','FAILED','ACKNOWLEDGED_PERSON') AND updated_at<=?", arrayOf((now - limits.terminalRetentionMillis).toString()))
        db.delete("fragments", "updated_at<=?", arrayOf((now - 10 * 60_000L).toString()))
        return expired
    }

    private fun evictLowestPriorityLocked(): Boolean {
        val cursor = db.rawQuery("SELECT id FROM records WHERE state IN ('QUEUED','RELAYED','TRANSFERRED') ORDER BY priority ASC, updated_at ASC LIMIT 1", null)
        val id = cursor.use { if (it.moveToFirst()) it.getString(0) else null } ?: return false
        db.delete("records", "id=?", arrayOf(id))
        return true
    }

    private fun stateLocked(id: UUID): DeliveryState? =
        db.rawQuery("SELECT state FROM records WHERE id=?", arrayOf(id.toString())).use {
            if (it.moveToFirst()) DeliveryState.valueOf(it.getString(0)) else null
        }

    private fun record(c: android.database.Cursor) = DeliveryRecord(
        UUID.fromString(c.getString(0)),
        Direction.valueOf(c.getString(1)),
        DeliveryState.valueOf(c.getString(2)),
        c.getBlob(3),
        c.getString(4),
        c.getString(5),
        M4Priority.entries.first { it.wire == c.getInt(6) },
        c.getLong(7),
        c.getInt(8),
        c.getInt(9),
        c.getInt(10),
        c.getLong(11),
    )

    private fun allowed(from: DeliveryState, to: DeliveryState) = when (from) {
        DeliveryState.CREATED -> to == DeliveryState.VALIDATED || to == DeliveryState.FAILED || to == DeliveryState.EXPIRED
        DeliveryState.VALIDATED -> to == DeliveryState.QUEUED || to == DeliveryState.FAILED || to == DeliveryState.EXPIRED
        DeliveryState.QUEUED -> to == DeliveryState.TRANSFERRED || to == DeliveryState.RELAYED || to == DeliveryState.EXPIRED || to == DeliveryState.FAILED
        DeliveryState.TRANSFERRED -> to == DeliveryState.RELAYED || to == DeliveryState.DELIVERED_DEVICE || to == DeliveryState.FAILED || to == DeliveryState.EXPIRED
        DeliveryState.RELAYED -> to == DeliveryState.TRANSFERRED || to == DeliveryState.DELIVERED_DEVICE || to == DeliveryState.EXPIRED || to == DeliveryState.FAILED
        DeliveryState.DELIVERED_DEVICE -> to == DeliveryState.PLAYBACK_STARTED || to == DeliveryState.FAILED
        DeliveryState.PLAYBACK_STARTED -> to == DeliveryState.ACKNOWLEDGED_PERSON || to == DeliveryState.FAILED
        DeliveryState.ACKNOWLEDGED_PERSON, DeliveryState.EXPIRED, DeliveryState.FAILED -> false
    }

    private inline fun <T> SQLiteDatabase.transaction(block: SQLiteDatabase.() -> T): T {
        beginTransaction()
        return try { block().also { setTransactionSuccessful() } } finally { endTransaction() }
    }

    private object Database {
        fun count(db: SQLiteDatabase): () -> Int =
            { db.rawQuery("SELECT COUNT(*) FROM records WHERE state IN ('QUEUED','RELAYED','TRANSFERRED')", null).use { it.moveToFirst(); it.getInt(0) } }

        fun bytes(db: SQLiteDatabase): () -> Long =
            { db.rawQuery("SELECT COALESCE(SUM(LENGTH(bundle)),0) FROM records WHERE state IN ('QUEUED','RELAYED','TRANSFERRED')", null).use { it.moveToFirst(); it.getLong(0) } }

        fun originCount(db: SQLiteDatabase, source: String): Int =
            db.rawQuery("SELECT COUNT(*) FROM records WHERE source_id=? AND state IN ('QUEUED','RELAYED','TRANSFERRED')", arrayOf(source)).use { it.moveToFirst(); it.getInt(0) }
    }
}
