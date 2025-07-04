package com.ywcheong.simple.transaction.account.domain

import com.ywcheong.simple.transaction.member.domain.MemberId

interface AccountRepository {
    fun findAccountById(accountId: AccountId): Account?
    fun findAccountByOwner(memberId: MemberId): List<Account>
    fun insert(account: Account): Unit
    fun update(account: Account): Unit
    fun delete(accountId: AccountId): Boolean
}