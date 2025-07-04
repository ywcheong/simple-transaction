package com.ywcheong.simple.transaction.account.domain

import com.ywcheong.simple.transaction.member.domain.MemberId

@JvmInline
value class AccountId(val value: String) {
    init {
        val uuid = try {
            java.util.UUID.fromString(value)
        } catch (e: IllegalArgumentException) {
            throw InvalidAccountIdException()
        }
        if (uuid.version() != 4) {
            throw InvalidAccountIdException()
        }
    }

    companion object {
        fun createUnique(): AccountId = AccountId(java.util.UUID.randomUUID().toString())
    }
}


@JvmInline
value class AccountBalance(val value: Long) {
    init {
        if (value < 0) throw NegativeBalanceException()
    }

    internal operator fun plus(change: AccountBalanceChange): AccountBalance = AccountBalance(value + change.value)

    internal operator fun minus(change: AccountBalanceChange): AccountBalance {
        if (value < change.value) throw InsufficientBalanceException()
        return AccountBalance(value - change.value)
    }
}

@JvmInline
value class AccountBalanceChange(val value: Long) {
    init {
        if (value <= 0) throw NonPositiveBalanceChangeException()
    }
}

data class Account(
    val id: AccountId, val owner: MemberId, val balance: AccountBalance, val version: Long
) {
    fun deposit(change: AccountBalanceChange): Account = this.copy(balance = balance + change)
    fun withdraw(change: AccountBalanceChange): Account = this.copy(balance = balance - change)
}