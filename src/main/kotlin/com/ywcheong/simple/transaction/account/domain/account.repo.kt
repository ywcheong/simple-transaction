package com.ywcheong.simple.transaction.account.domain

import com.ywcheong.simple.transaction.member.domain.MemberId

interface AccountRepository {
    fun findAccountById(accountId: AccountId): Account?
    fun findAccountByOwner(memberId: MemberId): List<Account>
    fun insert(account: Account)
    fun update(account: Account)
    fun delete(account: Account)
}