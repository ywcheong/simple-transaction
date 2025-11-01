package com.ywcheong.simple.transaction.teller.auto.domain

import com.ywcheong.simple.transaction.teller.auto.domain.cash.Cash
import com.ywcheong.simple.transaction.teller.base.domain.Teller

data class AutoTeller(
    val teller: Teller, val vault: Cash, val version: Long
) {
    fun deposit(cash: Cash): AutoTeller = this.copy(vault = vault + cash)
    fun withdraw(cash: Cash): AutoTeller = this.copy(vault = vault - cash)
    fun isZero(): Boolean = vault.isZero()
}