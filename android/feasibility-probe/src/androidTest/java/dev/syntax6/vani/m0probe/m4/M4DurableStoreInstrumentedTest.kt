package dev.syntax6.vani.m4

import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class M4DurableStoreInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var store: M4DeliveryStore

    @Before fun setUp() {
        context.deleteDatabase("vani_m4_delivery.db")
        store = M4DeliveryStore(context, QueueLimits(maxMessages = 2, maxBytes = 1024, perOriginMaxMessages = 64))
    }

    @After fun tearDown() {
        store.close()
        context.deleteDatabase("vani_m4_delivery.db")
    }

    @Test fun restartRecoversQueuedRecord() {
        val message = message(UUID.randomUUID(), 1000L, 10_000L, M4Priority.URGENT)
        val bytes = CanonicalBinaryBundleCodec().encode(message)
        store.enqueue(bytes, message, now = 1000L)
        val id = message.messageId
        store.close()
        store = M4DeliveryStore(context, QueueLimits(maxMessages = 2, maxBytes = 1024, perOriginMaxMessages = 64))
        assertEquals(DeliveryState.QUEUED, store.state(id))
        assertArrayEquals(bytes, store.get(id)!!.bundle)
    }

    @Test fun expiryIsDurableAndTruthful() {
        val message = message(UUID.randomUUID(), 1000L, 2000L, M4Priority.ROUTINE)
        store.enqueue(CanonicalBinaryBundleCodec().encode(message), message, now = 1000L)
        assertEquals(1, store.expire(2000L))
        assertEquals(DeliveryState.EXPIRED, store.state(message.messageId))
    }

    @Test fun duplicateSeenStateSurvivesRestart() {
        val id = UUID.randomUUID()
        assertTrue(store.markSeenIfNew(id, 10_000L, 1000L))
        assertFalse(store.markSeenIfNew(id, 10_000L, 1000L))
        store.close()
        store = M4DeliveryStore(context, QueueLimits(maxMessages = 2, maxBytes = 1024, perOriginMaxMessages = 64))
        assertFalse(store.markSeenIfNew(id, 10_000L, 1000L))
    }

    @Test fun queueEvictsLowestPriorityBeforeRejecting() {
        val routine = message(UUID.randomUUID(), 1000L, 10_000L, M4Priority.ROUTINE)
        val urgent = message(UUID.randomUUID(), 1000L, 10_000L, M4Priority.URGENT)
        val distress = message(UUID.randomUUID(), 1000L, 10_000L, M4Priority.DISTRESS)
        store.enqueue(ByteArray(100), routine, now = 1000L)
        store.enqueue(ByteArray(100), urgent, now = 1001L)
        store.enqueue(ByteArray(100), distress, now = 1002L)
        assertNull(store.get(routine.messageId))
        assertNotNull(store.get(urgent.messageId))
        assertNotNull(store.get(distress.messageId))
    }

    @Test fun fragmentRecordsSurviveRestart() {
        val id = UUID.randomUUID()
        val frame = M4Frame(id, 0, 2, 4, byteArrayOf(1, 2))
        store.saveFragment(frame, 1000L)
        store.close()
        store = M4DeliveryStore(context, QueueLimits(maxMessages = 2, maxBytes = 1024, perOriginMaxMessages = 64))
        assertEquals(listOf(frame), store.loadFragments(id))
    }

    private fun message(id: UUID, created: Long, expires: Long, priority: M4Priority) = SemanticBundle(
        messageId = id, sourceId = "sender", destination = "receiver", createdAtEpochMillis = created,
        expiresAtEpochMillis = expires, hopLimit = 4, languageTag = "en-IN", priority = priority,
        ackPolicy = M4AckPolicy.DEVICE_RECEIVED, safetyAction = M4SafetyAction.SEND, transcript = "test", copyBudget = 4
    )
}
