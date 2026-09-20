package dev.syntax6.vani.m0probe.m4

import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class M4DeliveryStoreInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before fun reset() {
        context.deleteDatabase("vani_m4.db")
    }

    @After fun cleanup() {
        context.deleteDatabase("vani_m4.db")
    }

    private fun stored(
        id: String,
        priority: M4Protocol.Priority = M4Protocol.Priority.ROUTINE,
        state: M4Protocol.DeliveryState = M4Protocol.DeliveryState.QUEUED,
        bytes: Int = 20,
        expiresAt: Long = System.currentTimeMillis() + 60_000
    ) = StoredBundle(
        id, "receiver", priority, System.currentTimeMillis(), expiresAt, 4, 8, 0, state, ByteArray(bytes)
    )

    @Test fun queueLimitEvictsOldestLowestPriority() {
        val store = M4DeliveryStore(context, maxItems = 2, maxBytes = 100)
        store.enqueue(stored("low"))
        store.enqueue(stored("urgent", M4Protocol.Priority.URGENT))
        store.enqueue(stored("new", M4Protocol.Priority.ROUTINE))
        assertNull(store.nextEligible("receiver")?.takeIf { it.messageId == "low" })
        assertNotNull(store.nextEligible("receiver"))
    }

    @Test fun restartRecoveryReturnsInFlightToQueue() {
        M4DeliveryStore(context).apply {
            enqueue(stored("restart", state = M4Protocol.DeliveryState.QUEUED))
            updateState("restart", M4Protocol.DeliveryState.TRANSFERRED)
            recoverAfterRestart()
            close()
        }
        val recovered = M4DeliveryStore(context).nextEligible("receiver")
        assertEquals("restart", recovered?.messageId)
        assertEquals(M4Protocol.DeliveryState.QUEUED, recovered?.state)
    }

    @Test fun expiredItemsBecomeTerminal() {
        val store = M4DeliveryStore(context)
        store.enqueue(stored("expired", expiresAt = 1))
        assertEquals(1, store.purgeExpired(2))
        assertEquals(M4Protocol.DeliveryState.EXPIRED, store.state("expired"))
    }

    @Test fun seenMessageIsDurableAndIdempotent() {
        val first = M4DeliveryStore(context)
        assertTrue(first.markSeen("replay"))
        assertTrue(!first.markSeen("replay"))
        first.close()
        val second = M4DeliveryStore(context)
        assertTrue(second.hasSeen("replay"))
    }
}
