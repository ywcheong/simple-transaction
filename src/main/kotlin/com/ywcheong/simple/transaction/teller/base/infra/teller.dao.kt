package com.ywcheong.simple.transaction.teller.base.infra

import com.ywcheong.simple.transaction.certification.domain.CertificationPublicKey
import com.ywcheong.simple.transaction.teller.base.TellerDaoAggregateStrategy
import com.ywcheong.simple.transaction.teller.base.domain.*
import org.seasar.doma.*
import org.seasar.doma.boot.ConfigAutowireable
import org.springframework.stereotype.Repository

@Entity
@Table(name = "teller")
class TellerEntity(
    @Id val id: String? = null,
    val type: Int? = null,
    val active: Boolean? = null,
    @Column(name = "public_key") val publicKey: String? = null,
    @Association val permissions: List<TellerPermissionEntity>? = null,
    @Version val version: Long? = null
) {
    constructor(teller: Teller) : this(
        id = teller.id.value,
        type = teller.type.value,
        active = teller.active,
        publicKey = teller.publicKey?.value,
        permissions = teller.permissions.map { TellerPermissionEntity(teller, it) },
        version = teller.version
    )

    fun toTeller(): Teller {
        if (id == null) throw UnexpectedTellerRepositoryNullException("teller.id")
        if (type == null) throw UnexpectedTellerRepositoryNullException("teller.type")
        if (active == null) throw UnexpectedTellerRepositoryNullException("teller.active")
        if (version == null) throw UnexpectedTellerRepositoryNullException("teller.version")

        return Teller(
            id = TellerId(id),
            type = TellerType(type),
            active = active,
            publicKey = publicKey?.let { CertificationPublicKey(it) },
            permissions = permissions?.map { it.toTellerPermission() }?.toSet() ?: setOf(),
            version = version
        )
    }
}

@Entity(immutable = true)
@Table(name = "teller_permission")
data class TellerPermissionEntity(
    val id: String,
    val permission: String,
) {
    constructor(
        teller: Teller, tellerPermission: TellerPermission
    ) : this(
        id = teller.id.value, permission = tellerPermission.value
    )

    fun toTellerPermission() = TellerPermission(permission)
}

@Dao
@ConfigAutowireable
interface TellerDao {
    @Select(aggregateStrategy = TellerDaoAggregateStrategy::class)
    @Sql(
        """
        SELECT
            /*%expand*/*
        FROM
            teller t
            LEFT JOIN teller_permission p
                ON t.id = p.id
        WHERE
            t.id = /* tellerId */'NaN'
                AND
            t.active = true
    """
    )
    fun select(tellerId: String): TellerEntity?

    @Insert
    fun insert(teller: TellerEntity): Int

    @Update
    fun update(teller: TellerEntity): Int
}

@Repository
class DefaultTellerRepository(
    private val dao: TellerDao
) : TellerRepository {
    override fun select(tellerId: TellerId): Teller? = dao.select(tellerId.value)?.toTeller()
    override fun insert(teller: Teller) {
        val insertCount = dao.insert(TellerEntity(teller))
        if (insertCount != 1) throw UnexpectedTellerRepositoryInsertException()
    }

    override fun update(teller: Teller) {
        val updateCount = dao.update(TellerEntity(teller))
        if (updateCount != 1) throw UnexpectedTellerRepositoryUpdateException()
    }

    override fun delete(teller: Teller) {
        update(teller.copy(active = false))
    }
}