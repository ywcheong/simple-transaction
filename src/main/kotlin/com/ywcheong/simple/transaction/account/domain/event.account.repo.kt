package com.ywcheong.simple.transaction.account.domain

interface AccountEventRepository {
    fun findById(eventId: AccountEventId): AccountEvent?
    fun insert(event: AccountEvent): Boolean
}

interface AccountEventOutboxRepository {
    fun findNotPublished(): List<AccountEvent>
    fun markAsPublished(events: List<AccountEvent>)
}