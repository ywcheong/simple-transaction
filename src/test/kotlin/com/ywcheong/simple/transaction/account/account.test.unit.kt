package com.ywcheong.simple.transaction.account.domain

import com.ywcheong.simple.transaction.member.domain.MemberId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class AccountIdTest {
    @Test
    fun `올바른 형식의 계좌번호는 생성된다`() {
        val uuid = java.util.UUID.randomUUID().toString()

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
            id = AccountId.createUnique(), owner = MemberId("abcdef"), balance = AccountBalance(1000), version = 1L
        )
        val change = AccountBalanceChange(500)

        val updated = account.deposit(change)

        assertEquals(1500, updated.balance.value)
    }

    @Test
    fun `출금 시 잔고가 감소한다`() {
        val account = Account(
            id = AccountId.createUnique(), owner = MemberId("abcdef"), balance = AccountBalance(1000), version = 1L
        )
        val change = AccountBalanceChange(400)

        val updated = account.withdraw(change)

        assertEquals(600, updated.balance.value)
    }

    @Test
    fun `잔고 전부를 출금할 수 있다`() {
        val account = Account(
            id = AccountId.createUnique(), owner = MemberId("abcdef"), balance = AccountBalance(1000), version = 1L
        )
        val change = AccountBalanceChange(1000)

        val updated = account.withdraw(change)

        assertEquals(0, updated.balance.value)
    }

    @Test
    fun `잔고보다 많은 금액을 출금할 수 없다`() {
        val account = Account(
            id = AccountId.createUnique(), owner = MemberId("abcdef"), balance = AccountBalance(300), version = 1L
        )
        val change = AccountBalanceChange(301)

        assertThrows<InsufficientBalanceException> {
            account.withdraw(change)
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
