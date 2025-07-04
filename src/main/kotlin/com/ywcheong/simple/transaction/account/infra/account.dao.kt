package com.ywcheong.simple.transaction.account.infra

import com.ywcheong.simple.transaction.account.domain.*
import com.ywcheong.simple.transaction.member.domain.MemberId
import org.seasar.doma.*
import org.seasar.doma.boot.ConfigAutowireable
import org.seasar.doma.jdbc.Result
import org.springframework.stereotype.Repository

@Entity(immutable = true)
@Table(name = "account")
class AccountEntity(
    @Id val id: String, val owner: String, val balance: Long
) {
    constructor(account: Account) : this(
        id = account.id.value, owner = account.owner.value, balance = account.balance.value
    )

    fun toAccount(): Account {
        return Account(
            id = AccountId(id),
            owner = MemberId(owner),
            balance = AccountBalance(balance),
        )
    }
}

@Dao
@ConfigAutowireable
interface AccountDao {
    @Select
    @Sql(
        """
        SELECT
            *
        FROM
            account
        WHERE
            id = /* id */'NaN'
    """
    )
    fun findById(id: String): AccountEntity?

    @Select
    @Sql(
        """
        SELECT
            *
        FROM
            account
        WHERE
            owner = /* owner */'NaN'
    """
    )
    fun findByOwner(owner: String): List<AccountEntity>

    @Insert
    fun insert(accountEntity: AccountEntity): Result<AccountEntity>

    @Update
    fun update(accountEntity: AccountEntity): Result<AccountEntity>

    @Delete
    @Sql(
        """
        DELETE
        FROM
            account
        WHERE
            id = /* id */'NaN'
    """
    )
    fun delete(id: String): Int
}

@Repository
class DefaultAccountRepository(
    private val dao: AccountDao
) : AccountRepository {
    override fun findAccountById(accountId: AccountId): Account? = dao.findById(accountId.value)?.toAccount()

    override fun findAccountByOwner(memberId: MemberId): List<Account> =
        dao.findByOwner(memberId.value).map { it.toAccount() }

    override fun insert(account: Account) {
        val insertCount = dao.insert(AccountEntity(account)).count
        if (insertCount != 1) throw UnexpectedAccountRepositoryFailedException()
    }

    override fun update(account: Account) {
        val updateCount = dao.update(AccountEntity(account)).count
        if (updateCount != 1) throw UnexpectedAccountRepositoryFailedException()
    }

    override fun delete(accountId: AccountId): Boolean {
        val deleteCount = dao.delete(accountId.value)
        return deleteCount == 1
    }
}