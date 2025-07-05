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
value class AccountPendingBalance(val value: Long) {
    init {
        if (value < 0) throw NegativeBalanceException()
    }

    internal operator fun plus(change: AccountBalanceChange): AccountPendingBalance = AccountPendingBalance(
        value + change.value
    )

    internal operator fun minus(change: AccountBalanceChange): AccountPendingBalance {
        if (value < change.value) throw UnexpectedPendingBalanceInsufficientException()
        return AccountPendingBalance(value - change.value)
    }
}

@JvmInline
value class AccountBalanceChange(val value: Long) {
    init {
        if (value <= 0) throw NonPositiveBalanceChangeException()
    }
}

data class Account(
    val id: AccountId,
    val owner: MemberId,
    val balance: AccountBalance,
    val pendingBalance: AccountPendingBalance,
    val version: Long
) {
    fun deposit(change: AccountBalanceChange): Account = this.copy(balance = balance + change)
    fun withdraw(change: AccountBalanceChange): Account = this.copy(balance = balance - change)

    fun pend(change: AccountBalanceChange): Account =
        this.copy(balance = balance - change, pendingBalance = pendingBalance + change)

    fun release(change: AccountBalanceChange): Account =
        this.copy(balance = balance + change, pendingBalance = pendingBalance - change)
}

data class Transfer(
    val fromAccount: Account, val toAccount: Account, val amount: AccountBalanceChange
) {
    private fun isPendingRequired(): Boolean = when {
        fromAccount.owner == toAccount.owner -> false
        else -> amount.value >= LARGE_TRANSFER_THRESHOLD.value
    }

    fun execute(): TransferResult = when (isPendingRequired()) {
        true -> executePendingTransfer()
        false -> executeImmediateTransfer()
    }

    private fun executePendingTransfer(): TransferResult {
        val newFromAccount = fromAccount.pend(amount)
        return TransferResult.Pending(newFromAccount, toAccount, amount)
    }

    private fun executeImmediateTransfer(): TransferResult {
        val newFromAccount = fromAccount.withdraw(amount)
        val newToAccount = toAccount.deposit(amount)
        return TransferResult.Complete(newFromAccount, newToAccount)
    }

    companion object {
        val LARGE_TRANSFER_THRESHOLD = AccountBalanceChange(1_000_000)
    }
}

sealed class TransferResult {
    data class Complete(val fromAccount: Account, val toAccount: Account) : TransferResult()
    data class Pending(val fromAccount: Account, val toAccount: Account, val amount: AccountBalanceChange) :
        TransferResult() {
        fun approve(): Complete {
            val newFromAccount = fromAccount.release(amount).withdraw(amount)
            val newToAccount = toAccount.deposit(amount)
            return Complete(newFromAccount, newToAccount)
        }

        fun reject(): Complete {
            val newFromAccount = fromAccount.release(amount)
            return Complete(newFromAccount, toAccount)
        }
    }
}