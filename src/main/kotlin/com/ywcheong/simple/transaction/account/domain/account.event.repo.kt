package com.ywcheong.simple.transaction.account.domain

interface AccountEventRepository {
    fun selectById(eventId: AccountEventId): AccountEvent?
    fun insert(event: AccountEvent): Boolean
}