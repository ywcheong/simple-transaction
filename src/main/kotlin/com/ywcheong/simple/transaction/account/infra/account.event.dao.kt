package com.ywcheong.simple.transaction.account.infra

import com.ywcheong.simple.transaction.account.domain.*
import org.seasar.doma.*
import org.seasar.doma.boot.ConfigAutowireable
import org.seasar.doma.jdbc.Result
import org.springframework.stereotype.Repository
import java.util.*

@Entity(immutable = true)
data class AccountEventEntity(
    @Id val id: String,
    val type: Int,
    val account: String? = null,
    @Column(name = "account_from") val accountFrom: String? = null,
    @Column(name = "account_to") val accountTo: String? = null,
    val amount: Long? = null,
    @Column(name = "previous_id") val previousId: String? = null,
    val reason: String? = null,
    @Column(name = "issued_at") val issuedAt: Date,
) {
    // AccountEvent → Entity 생성자
    constructor(event: AccountEvent) : this(
        id = event.id.value,
        type = event.type.type,
        account = event.account?.value,
        accountFrom = event.accountFrom?.value,
        accountTo = event.accountTo?.value,
        amount = event.amount?.value,
        reason = event.reason,
        issuedAt = event.issuedAt
    )

    // Entity → AccountEvent 변환
    fun toAccountEvent(): AccountEvent = when (AccountEventType(type)) {
        AccountEventType.DEPOSIT -> AccountDepositedEvent(
            id = AccountEventId(id),
            issuedAt = issuedAt,
            account = AccountId(requireNotNull(account)),
            amount = AccountBalanceChange(requireNotNull(amount))
        )

        AccountEventType.WITHDRAW -> AccountWithdrewEvent(
            id = AccountEventId(id),
            issuedAt = issuedAt,
            account = AccountId(requireNotNull(account)),
            amount = AccountBalanceChange(requireNotNull(amount))
        )

        AccountEventType.TRANSFER_ATTEMPT -> AccountTransferAttemptEvent(
            id = AccountEventId(id),
            issuedAt = issuedAt,
            accountFrom = AccountId(requireNotNull(accountFrom)),
            accountTo = AccountId(requireNotNull(accountTo)),
            amount = AccountBalanceChange(requireNotNull(amount))
        )

        AccountEventType.TRANSFER_ACCEPT -> AccountTransferAcceptedEvent(
            id = AccountEventId(id),
            previousId = AccountEventId(requireNotNull(previousId)),
            issuedAt = issuedAt,
            accountFrom = AccountId(requireNotNull(accountFrom)),
            accountTo = AccountId(requireNotNull(accountTo)),
            amount = AccountBalanceChange(requireNotNull(amount))
        )

        AccountEventType.TRANSFER_REJECT -> AccountTransferRejectedEvent(
            id = AccountEventId(id),
            previousId = AccountEventId(requireNotNull(previousId)),
            issuedAt = issuedAt,
            accountFrom = AccountId(requireNotNull(accountFrom)),
            accountTo = AccountId(requireNotNull(accountTo)),
            amount = AccountBalanceChange(requireNotNull(amount)),
            reason = requireNotNull(reason)
        )

        else -> throw UnexpectedAccountEventTypeException(type)
    }
}


@Dao
@ConfigAutowireable
interface AccountEventDao {
    @Select
    @Sql(
        """
        SELECT
            *
        FROM
            account_event
        WHERE
            id = /* id */'NaN'
    """
    )
    fun findById(id: String): AccountEventEntity?

    @Insert
    fun insert(event: AccountEventEntity): Result<AccountEventEntity>
}

@Repository
class DefaultAccountEventRepository(
    private val dao: AccountEventDao
) : AccountEventRepository {
    override fun findById(eventId: AccountEventId): AccountEvent? = dao.findById(eventId.value)?.toAccountEvent()
    override fun insert(event: AccountEvent): Boolean = dao.insert(AccountEventEntity(event)).count == 1
}
