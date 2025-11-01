package com.ywcheong.simple.transaction.teller.auto.domain

import com.ywcheong.simple.transaction.teller.base.domain.TellerId

interface AutoTellerRepository {
    fun select(tellerId: TellerId): AutoTeller?

    fun insert(autoTeller: AutoTeller)
    fun update(autoTeller: AutoTeller)
}