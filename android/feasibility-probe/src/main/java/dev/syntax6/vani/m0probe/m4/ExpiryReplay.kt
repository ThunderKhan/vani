package dev.syntax6.vani.m4

import java.util.UUID

data class ReplayDecision(val accepted: Boolean, val reason: String)

interface SeenMessageStore {
    fun markIfNew(id: UUID, expiresAt: Long, now: Long): Boolean
}

class ReplayGuard(private val store: SeenMessageStore) {
    fun accept(id: UUID, expiresAt: Long, now: Long): ReplayDecision =
        if (now >= expiresAt) ReplayDecision(false, "expired")
        else if (store.markIfNew(id, expiresAt, now)) ReplayDecision(true, "new")
        else ReplayDecision(false, "replay-or-duplicate")
}
