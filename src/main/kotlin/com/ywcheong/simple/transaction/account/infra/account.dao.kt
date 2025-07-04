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
    @Id val id: String,
    val owner: String,
    val balance: Long,
    @Version val version: Long,
    @Column(name = "is_withdrew") val isWithdrew: Boolean
) {
    constructor(account: Account, isWithdrew: Boolean = false) : this(
        id = account.id.value,
        owner = account.owner.value,
        balance = account.balance.value,
        version = account.version,
        isWithdrew = isWithdrew
    )

    fun toAccount(): Account {
        return Account(
            id = AccountId(id),
            owner = MemberId(owner),
            balance = AccountBalance(balance),
            version = version
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
                AND
            is_withdrew = FALSE
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
                AND
            is_withdrew = FALSE
    """
    )
    fun findByOwner(owner: String): List<AccountEntity>

    @Insert
    fun insert(accountEntity: AccountEntity): Result<AccountEntity>

    @Update(ignoreVersion = false)  // 낙관적 동시성 제어
    fun update(accountEntity: AccountEntity): Result<AccountEntity>
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

    override fun delete(account: Account) {
        // 이름은 Delete 이지만 실제로는 감사 목적으로 Delete 대신 불리언 플래그만 비활성화 처리함
        // 이것은 구현에 따른 숨겨진 로직이므로, 컨트롤러에는 Delete 인터페이스로 제공
        val accountEntity = AccountEntity(account, isWithdrew = true)
        val updateCount = dao.update(accountEntity).count
        if (updateCount != 1) throw UnexpectedAccountRepositoryFailedException()
    }
}