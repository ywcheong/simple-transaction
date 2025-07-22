package com.ywcheong.simple.transaction.account.infra

import com.ywcheong.simple.transaction.account.domain.*
import com.ywcheong.simple.transaction.member.domain.MemberId
import org.seasar.doma.*
import org.seasar.doma.boot.ConfigAutowireable
import org.seasar.doma.jdbc.BatchResult
import org.seasar.doma.jdbc.Result
import org.springframework.stereotype.Repository
import java.util.*

@Entity(immutable = true)
@Table(name = "account_event")
data class AccountEventEntity(
    @Id val id: String,
    @Column(name = "event_type") val type: Int,
    val account: String? = null,
    @Column(name = "account_from") val accountFrom: String? = null,
    @Column(name = "account_to") val accountTo: String? = null,
    val amount: Long? = null,
    @Column(name = "subsequent_id") val subsequentId: String? = null,
    val reason: String? = null,
    @Column(name = "issued_at") val issuedAt: Date,
    @Column(name = "issued_by") val issuedBy: String,
    @Column(name = "published_at") val publishedAt: Date?
) {
    // AccountEvent → Entity 생성자
    constructor(event: AccountEvent, publishedAt: Date? = null) : this(
        id = event.id.value,
        type = event.type.type,
        account = event.account?.value,
        accountFrom = event.accountFrom?.value,
        accountTo = event.accountTo?.value,
        amount = event.amount?.value,
        reason = event.reason,
        issuedAt = event.issuedAt,
        issuedBy = event.issuedBy.value,
        publishedAt = publishedAt
    )

    // Entity → AccountEvent 변환
    fun toAccountEvent(): AccountEvent = when (AccountEventType(type)) {
        AccountEventType.DEPOSIT -> AccountDepositedEvent(
            id = AccountEventId(id),
            issuedAt = issuedAt,
            account = AccountId(requireNotNull(account)),
            amount = AccountBalanceChange(requireNotNull(amount)),
            issuedBy = MemberId(issuedBy)
        )

        AccountEventType.WITHDRAW -> AccountWithdrewEvent(
            id = AccountEventId(id),
            issuedAt = issuedAt,
            account = AccountId(requireNotNull(account)),
            amount = AccountBalanceChange(requireNotNull(amount)),
            issuedBy = MemberId(issuedBy)
        )

        AccountEventType.TRANSFER_ATTEMPT -> AccountTransferAttemptEvent(
            id = AccountEventId(id),
            issuedAt = issuedAt,
            accountFrom = AccountId(requireNotNull(accountFrom)),
            accountTo = AccountId(requireNotNull(accountTo)),
            amount = AccountBalanceChange(requireNotNull(amount)),
            issuedBy = MemberId(issuedBy),
            subsequentId = subsequentId?.let { AccountEventId(it) })

        AccountEventType.TRANSFER_ACCEPT -> AccountTransferAcceptedEvent(
            id = AccountEventId(id),
            issuedAt = issuedAt,
            accountFrom = AccountId(requireNotNull(accountFrom)),
            accountTo = AccountId(requireNotNull(accountTo)),
            amount = AccountBalanceChange(requireNotNull(amount)),
            issuedBy = MemberId(issuedBy)
        )

        AccountEventType.TRANSFER_REJECT -> AccountTransferRejectedEvent(
            id = AccountEventId(id),
            issuedAt = issuedAt,
            accountFrom = AccountId(requireNotNull(accountFrom)),
            accountTo = AccountId(requireNotNull(accountTo)),
            amount = AccountBalanceChange(requireNotNull(amount)),
            reason = requireNotNull(reason),
            issuedBy = MemberId(issuedBy)
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

    @Select
    @Sql(
        """
        SELECT
            *
        FROM
            account_event
        WHERE
            published_at IS NULL
        """
    )
    fun findNotPublished(): List<AccountEventEntity>

    @BatchUpdate
    fun updateBatch(events: List<AccountEventEntity>): BatchResult<AccountEventEntity>

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

@Repository
class DefaultAccountEventOutboxRepository(
    private val dao: AccountEventDao
) : AccountEventOutboxRepository {
    override fun findNotPublished(): List<AccountEvent> = dao.findNotPublished().map { it.toAccountEvent() }
    override fun markAsPublished(events: List<AccountEvent>, publishedAt: Date) {
        dao.updateBatch(events.map { AccountEventEntity(it, publishedAt) })
    }
}