package dev.syntax6.vani.m0probe.m4

interface M4Store {
    fun enqueue(bundle: StoredBundle)
    fun nextEligible(destinationId: String?, now: Long = System.currentTimeMillis()): StoredBundle?
    fun state(messageId: String): M4Protocol.DeliveryState?
    fun updateState(messageId: String, state: M4Protocol.DeliveryState, attempts: Int? = null)
    fun remove(messageId: String)
    fun markSeen(messageId: String, seenAt: Long = System.currentTimeMillis()): Boolean
    fun hasSeen(messageId: String): Boolean
    fun recoverAfterRestart()
    fun purgeExpired(now: Long = System.currentTimeMillis()): Int
    fun saveFragment(fragment: M4Protocol.Fragment, receivedAt: Long = System.currentTimeMillis())
    fun hasFragment(messageId: String, index: Int): Boolean
    fun loadFragments(messageId: String): List<M4Protocol.Fragment>
    fun clearFragments(messageId: String)
    fun persistInbox(messageId: String, payload: ByteArray, state: M4Protocol.DeliveryState)
    fun inboxState(messageId: String): M4Protocol.DeliveryState?
    fun updateInboxState(messageId: String, state: M4Protocol.DeliveryState)
}
