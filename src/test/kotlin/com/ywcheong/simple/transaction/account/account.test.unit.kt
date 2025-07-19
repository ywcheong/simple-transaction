package com.ywcheong.simple.transaction.account.domain

import com.ywcheong.simple.transaction.member.domain.MemberId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import kotlin.test.assertEquals

class AccountIdTest {
    @Test
    fun `올바른 형식의 계좌번호는 생성된다`() {
        val uuid = UUID.randomUUID().toString()

        val accountId = AccountId(uuid)

        assertEquals(uuid, accountId.value)
    }

    @Test
    fun `올바르지 않은 형식의 계좌번호는 생성되지 않는다 - UUID 아님`() {
        val invalid = "not-a-uuid"

        assertThrows<InvalidAccountIdException> {
            AccountId(invalid)
        }
    }

    @Test
    fun `올바르지 않은 형식의 계좌번호는 생성되지 않는다 - UUID Version 4 아님`() {
        val uuid1 = "00000000-0000-0000-0000-000000000000"

        assertThrows<InvalidAccountIdException> {
            AccountId(uuid1)
        }
    }
}

class AccountBalanceTest {
    @Test
    fun `0 이상의 잔고는 생성된다`() {
        val balance = AccountBalance(0)

        assertEquals(0, balance.value)
    }

    @Test
    fun `음수 잔고는 생성되지 않는다`() {
        assertThrows<NegativeBalanceException> {
            AccountBalance(-1)
        }
    }

    @Test
    fun `입금 시 잔고가 증가한다`() {
        val account = Account(
            id = AccountId.createUnique(),
            owner = MemberId("abcdef"),
            balance = AccountBalance(1000),
            version = 1L,
            pendingBalance = AccountPendingBalance(0)
        )
        val change = AccountBalanceChange(500)

        val updated = account.deposit(change)

        assertEquals(1500, updated.balance.value)
    }

    @Test
    fun `출금 시 잔고가 감소한다`() {
        val account = Account(
            id = AccountId.createUnique(),
            owner = MemberId("abcdef"),
            balance = AccountBalance(1000),
            version = 1L,
            pendingBalance = AccountPendingBalance(0)
        )
        val change = AccountBalanceChange(400)

        val updated = account.withdraw(change)

        assertEquals(600, updated.balance.value)
    }

    @Test
    fun `잔고 전부를 출금할 수 있다`() {
        val account = Account(
            id = AccountId.createUnique(),
            owner = MemberId("abcdef"),
            balance = AccountBalance(1000),
            version = 1L,
            pendingBalance = AccountPendingBalance(0)
        )
        val change = AccountBalanceChange(1000)

        val updated = account.withdraw(change)

        assertEquals(0, updated.balance.value)
    }

    @Test
    fun `잔고보다 많은 금액을 출금할 수 없다`() {
        val account = Account(
            id = AccountId.createUnique(),
            owner = MemberId("abcdef"),
            balance = AccountBalance(300),
            version = 1L,
            pendingBalance = AccountPendingBalance(0)
        )
        val change = AccountBalanceChange(301)

        assertThrows<InsufficientBalanceException> {
            account.withdraw(change)
        }
    }

    @Test
    fun `송금대기 설정 시 잔고는 감소하고 보류잔고는 증가한다`() {
        val account = Account(
            id = AccountId.createUnique(),
            owner = MemberId("abcdef"),
            balance = AccountBalance(1000),
            pendingBalance = AccountPendingBalance(100),
            version = 1L
        )

        val updated = account.pend(AccountBalanceChange(300))

        assertEquals(700, updated.balance.value)
        assertEquals(400, updated.pendingBalance.value)
    }

    @Test
    fun `송금대기 해제 시 잔고는 증가하고 보류잔고는 감소한다`() {
        val account = Account(
            id = AccountId.createUnique(),
            owner = MemberId("abcdef"),
            balance = AccountBalance(700),
            pendingBalance = AccountPendingBalance(400),
            version = 1L
        )

        val updated = account.release(AccountBalanceChange(300))

        assertEquals(1000, updated.balance.value)
        assertEquals(100, updated.pendingBalance.value)
    }

    @Test
    fun `송금대기 해제 시 현재 보류잔고보다 많은 금액을 보류해제할 수 없다`() {
        val account = Account(
            id = AccountId.createUnique(),
            owner = MemberId("abcdef"),
            balance = AccountBalance(700),
            pendingBalance = AccountPendingBalance(400),
            version = 1L
        )

        assertThrows<UnexpectedPendingBalanceInsufficientException> {
            account.release(AccountBalanceChange(600))
        }
    }
}

class AccountBalanceChangeTest {
    @Test
    fun `양수 거래 단위는 생성된다`() {
        val change = AccountBalanceChange(1)

        assertEquals(1, change.value)
    }

    @Test
    fun `0 이하의 거래 단위는 생성되지 않는다`() {
        assertThrows<NonPositiveBalanceChangeException> {
            AccountBalanceChange(0)
        }
        assertThrows<NonPositiveBalanceChangeException> {
            AccountBalanceChange(-100)
        }
    }
}

class TransferTest {
    @Test
    fun `같은 소유주 간 송금은 고액이어도 즉시 처리된다`() {
        // Arrange
        val owner = MemberId("usersome")
        val fromAccount = Account(
            id = AccountId.createUnique(),
            owner = owner,
            balance = AccountBalance(3_000_000),
            pendingBalance = AccountPendingBalance(0),
            version = 1L
        )
        val toAccount = Account(
            id = AccountId.createUnique(),
            owner = owner,
            balance = AccountBalance(0),
            pendingBalance = AccountPendingBalance(0),
            version = 1L
        )
        val transfer = Transfer(fromAccount, toAccount, AccountBalanceChange(1_000_000))

        val result = transfer.execute()

        assert(result is TransferResult.Complete)
        val complete = result as TransferResult.Complete
        assertEquals(2_000_000, complete.fromAccount.balance.value)
        assertEquals(0, complete.fromAccount.pendingBalance.value)
        assertEquals(1_000_000, complete.toAccount.balance.value)
        assertEquals(0, complete.toAccount.pendingBalance.value)
    }

    @Test
    fun `다른 소유주에게 100만원 이상 송금은 고액송금으로 보류된다`() {
        val fromOwner = MemberId("ffffff")
        val toOwner = MemberId("tttttt")
        val fromAccount =
            Account(AccountId.createUnique(), fromOwner, AccountBalance(3_000_000), AccountPendingBalance(0), 1L)
        val toAccount = Account(AccountId.createUnique(), toOwner, AccountBalance(0), AccountPendingBalance(0), 1L)
        val transfer = Transfer(fromAccount, toAccount, AccountBalanceChange(1_000_000))

        val result = transfer.execute()

        assert(result is TransferResult.Pending)
        val pending = result as TransferResult.Pending
        assertEquals(2_000_000, pending.fromAccount.balance.value)
        assertEquals(1_000_000, pending.fromAccount.pendingBalance.value)
        assertEquals(0, pending.toAccount.balance.value)
        assertEquals(0, pending.toAccount.pendingBalance.value)
    }

    @Test
    fun `다른 소유주에게 100만원 미만 송금은 즉시 처리된다`() {
        // Arrange
        val fromOwner = MemberId("ffffff")
        val toOwner = MemberId("tttttt")
        val fromAccount =
            Account(AccountId.createUnique(), fromOwner, AccountBalance(3_000_000), AccountPendingBalance(0), 1L)
        val toAccount = Account(AccountId.createUnique(), toOwner, AccountBalance(0), AccountPendingBalance(0), 1L)
        val transfer = Transfer(fromAccount, toAccount, AccountBalanceChange(999_999))
        // Act
        val result = transfer.execute()
        // Assert
        assert(result is TransferResult.Complete)
        val complete = result as TransferResult.Complete
        assertEquals(2_000_001, complete.fromAccount.balance.value)
        assertEquals(0, complete.fromAccount.pendingBalance.value)
        assertEquals(999_999, complete.toAccount.balance.value)
        assertEquals(0, complete.toAccount.pendingBalance.value)
    }

    @Test
    fun `고액송금 대기 상태에서 송금승인 시 송금이 완료된다`() {
        // Arrange
        val fromOwner = MemberId("ffffff")
        val toOwner = MemberId("tttttt")
        val fromAccount =
            Account(AccountId.createUnique(), fromOwner, AccountBalance(3_000_000), AccountPendingBalance(0), 1L)
        val toAccount = Account(AccountId.createUnique(), toOwner, AccountBalance(0), AccountPendingBalance(0), 1L)
        val transfer = Transfer(fromAccount, toAccount, AccountBalanceChange(1_000_000))

        val pending = transfer.execute() as TransferResult.Pending
        val complete = pending.approve()

        assertEquals(2_000_000, complete.fromAccount.balance.value)
        assertEquals(0, complete.fromAccount.pendingBalance.value)
        assertEquals(1_000_000, complete.toAccount.balance.value)
        assertEquals(0, complete.toAccount.pendingBalance.value)
    }

    @Test
    fun `고액송금 대기 상태에서 송금거절 시 송금이 취소된다`() {
        // Arrange
        val fromOwner = MemberId("ffffff")
        val toOwner = MemberId("tttttt")
        val fromAccount =
            Account(AccountId.createUnique(), fromOwner, AccountBalance(3_000_000), AccountPendingBalance(0), 1L)
        val toAccount = Account(AccountId.createUnique(), toOwner, AccountBalance(0), AccountPendingBalance(0), 1L)
        val transfer = Transfer(fromAccount, toAccount, AccountBalanceChange(1_000_000))

        val pending = transfer.execute() as TransferResult.Pending
        val complete = pending.reject()

        assertEquals(3_000_000, complete.fromAccount.balance.value)
        assertEquals(0, complete.fromAccount.pendingBalance.value)
        assertEquals(0, complete.toAccount.balance.value)
        assertEquals(0, complete.toAccount.pendingBalance.value)
    }
}
