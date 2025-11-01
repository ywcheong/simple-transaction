package com.ywcheong.simple.transaction.teller.base.domain

import com.ywcheong.simple.transaction.teller.auto.domain.AutoTeller

interface TellerRepository {
    fun select(tellerId: TellerId): Teller?
    fun insert(teller: Teller)
    fun update(teller: Teller)
    fun delete(teller: Teller)
}