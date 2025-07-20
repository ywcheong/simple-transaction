package com.ywcheong.simple.transaction.account.domain

import com.ywcheong.simple.transaction.member.domain.MemberId
import java.util.*

@JvmInline
value class AccountEventId(val value: String) {
    companion object {
        fun createUnique(): AccountEventId = AccountEventId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class AccountEventType(val type: Int) {
    init {
        require(type in ALLOWED_VALUES) { "계좌 이벤트 타입의 값이 범위를 이탈했습니다. (값 = $type)" }
    }

    companion object {
        private val ALLOWED_VALUES = (0..4).toSet()
        val DEPOSIT = AccountEventType(0)
        val WITHDRAW = AccountEventType(1)
        val TRANSFER_ATTEMPT = AccountEventType(2)
        val TRANSFER_ACCEPT = AccountEventType(3)
        val TRANSFER_REJECT = AccountEventType(4)
    }
}

interface AccountEvent {
    val id: AccountEventId
    val issuedAt: Date
    val issuedBy: MemberId
    val type: AccountEventType

    val account: AccountId?
    val accountFrom: AccountId?
    val accountTo: AccountId?
    val amount: AccountBalanceChange?
    val subsequentId: AccountEventId?
    val reason: String?
}

data class AccountDepositedEvent(
    override val id: AccountEventId,
    override val issuedAt: Date,
    override val issuedBy: MemberId,
    override val account: AccountId,
    override val amount: AccountBalanceChange,
) : AccountEvent {
    override val type = AccountEventType.DEPOSIT
    override val accountFrom: AccountId? = null
    override val accountTo: AccountId? = null
    override val subsequentId: AccountEventId? = null
    override val reason: String? = null
}

data class AccountWithdrewEvent(
    override val id: AccountEventId,
    override val issuedAt: Date,
    override val issuedBy: MemberId,
    override val account: AccountId,
    override val amount: AccountBalanceChange,
) : AccountEvent {
    override val type = AccountEventType.WITHDRAW
    override val accountFrom: AccountId? = null
    override val accountTo: AccountId? = null
    override val subsequentId: AccountEventId? = null
    override val reason: String? = null
}

data class AccountTransferAttemptEvent(
    override val id: AccountEventId,
    override val issuedAt: Date,
    override val issuedBy: MemberId,
    override val accountFrom: AccountId,
    override val accountTo: AccountId,
    override val amount: AccountBalanceChange,
    override val subsequentId: AccountEventId?
) : AccountEvent {
    override val type = AccountEventType.TRANSFER_ATTEMPT
    override val account: AccountId? = null
    override val reason: String? = null
}

data class AccountTransferAcceptedEvent(
    override val id: AccountEventId,
    override val issuedAt: Date,
    override val issuedBy: MemberId,
    override val accountFrom: AccountId,
    override val accountTo: AccountId,
    override val amount: AccountBalanceChange,
) : AccountEvent {
    override val type = AccountEventType.TRANSFER_ACCEPT
    override val account: AccountId? = null
    override val subsequentId: AccountEventId? = null
    override val reason: String? = null
}

data class AccountTransferRejectedEvent(
    override val id: AccountEventId,
    override val issuedAt: Date,
    override val issuedBy: MemberId,
    override val accountFrom: AccountId,
    override val accountTo: AccountId,
    override val amount: AccountBalanceChange,
    override val reason: String,
) : AccountEvent {
    override val type = AccountEventType.TRANSFER_REJECT
    override val subsequentId: AccountEventId? = null
    override val account: AccountId? = null
}
