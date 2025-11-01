package com.ywcheong.simple.transaction.teller.auto.infra

import com.ywcheong.simple.transaction.teller.auto.AutoTellerDaoAggregateStrategy
import com.ywcheong.simple.transaction.teller.auto.domain.AutoTeller
import com.ywcheong.simple.transaction.teller.auto.domain.AutoTellerRepository
import com.ywcheong.simple.transaction.teller.auto.domain.cash.Cash
import com.ywcheong.simple.transaction.teller.base.domain.TellerId
import com.ywcheong.simple.transaction.teller.base.domain.UnexpectedTellerRepositoryInsertException
import com.ywcheong.simple.transaction.teller.base.domain.UnexpectedTellerRepositoryNullException
import com.ywcheong.simple.transaction.teller.base.domain.UnexpectedTellerRepositoryUpdateException
import com.ywcheong.simple.transaction.teller.base.infra.TellerEntity
import org.seasar.doma.*
import org.seasar.doma.boot.ConfigAutowireable
import org.springframework.stereotype.Repository

@Entity
@Table(name = "auto_teller")
data class AutoTellerEntity(
    @Id val id: String? = null,
    val vault: Long? = null,
    @Association val teller: TellerEntity? = null,
    @Version val version: Long? = null,
) {
    constructor(autoTeller: AutoTeller) : this(
        id = autoTeller.teller.id.value,
        vault = autoTeller.vault.value,
        teller = TellerEntity(autoTeller.teller),
        version = autoTeller.version
    )

    fun toAutoTeller(): AutoTeller {
        if (vault == null) throw UnexpectedTellerRepositoryNullException("autoteller.vault")
        if (teller == null) throw UnexpectedTellerRepositoryNullException("autoteller.teller")
        if (version == null) throw UnexpectedTellerRepositoryNullException("autoteller.version")

        return AutoTeller(
            teller = teller.toTeller(), vault = Cash(vault), version = version
        )
    }
}

@Dao
@ConfigAutowireable
interface AutoTellerDao {
    @Select(aggregateStrategy = AutoTellerDaoAggregateStrategy::class)
    @Sql(
        """
        SELECT
            /*%expand*/*
        FROM
            auto_teller a
            JOIN teller t
                ON a.id = t.id
            LEFT JOIN teller_permission p
                ON t.id = p.id
        WHERE
            a.id = /* tellerId */'NaN'
                AND
            t.active = true
    """
    )
    fun select(tellerId: String): AutoTellerEntity?

    @Insert
    fun insert(teller: AutoTellerEntity): Int

    @Update
    fun update(teller: AutoTellerEntity): Int
}

@Repository
class DefaultAutoTellerRepository(
    private val autoTellerDao: AutoTellerDao
) : AutoTellerRepository {
    override fun select(tellerId: TellerId): AutoTeller? = autoTellerDao.select(tellerId.value)?.toAutoTeller()

    override fun insert(autoTeller: AutoTeller) {
        val insertCount = autoTellerDao.insert(AutoTellerEntity(autoTeller))
        if (insertCount != 1) throw UnexpectedTellerRepositoryInsertException()
    }

    override fun update(autoTeller: AutoTeller) {
        val updateCount = autoTellerDao.update(AutoTellerEntity(autoTeller))
        if (updateCount != 1) throw UnexpectedTellerRepositoryUpdateException()
    }
}