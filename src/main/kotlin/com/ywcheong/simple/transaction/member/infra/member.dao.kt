package com.ywcheong.simple.transaction.member.infra

import com.ywcheong.simple.transaction.member.domain.*
import org.seasar.doma.*
import org.seasar.doma.boot.ConfigAutowireable
import org.springframework.stereotype.Repository

@Entity
@Table(name = "member")
class MemberEntity(
    @Id var id: String? = null,
    var name: String? = null,
    var phone: String? = null,
    var password: String? = null,
    var status: Int? = null
) {
    constructor(member: Member) : this(
        id = member.id.value,
        name = member.name.value,
        phone = member.phone.value,
        password = member.password.value,
        status = member.status.value
    )

    override fun toString(): String {
        return "MemberEntity[id=$id,name=$name,phone=$phone,password=MASKED,status=$status]"
    }

    fun toMember(): Member {
        require(id != null && name != null && phone != null && password != null && status != null) {
            "MemberEntity에 Null 값이 있으므로 Member로 변환할 수 없습니다. (${this.toString()})"
        }

        return Member(
            id = MemberId(id!!),
            name = MemberName(name!!),
            phone = MemberPhone(phone!!),
            password = MemberHashedPassword(password!!),
            status = MemberStatus(status!!),
        )
    }
}

@Dao
@ConfigAutowireable
interface MemberDao {
    @Sql(
        """
        SELECT
            *
        FROM
            member
        WHERE
            id = /* id */'00000000-0000-0000-0000-000000000000'
    """
    )
    @Select
    fun findById(id: String): MemberEntity?

    @Insert
    fun insert(memberEntity: MemberEntity): Int

    @Sql(
        """
        DELETE
        FROM
            member
        WHERE
            id = /* id */'00000000-0000-0000-0000-000000000000'
    """
    )
    @Delete
    fun delete(id: String): Int
}

@Repository
class DefaultMemberRepository(
    private val dao: MemberDao
) : MemberRepository {
    override fun findById(memberId: MemberId): Member? = dao.findById(memberId.value)?.toMember()

    override fun delete(memberId: MemberId): Boolean {
        val deleteCount = dao.delete(memberId.value)
        return deleteCount == 1
    }

    override fun insert(member: Member): Boolean {
        val insertCount = dao.insert(MemberEntity(member))
        return insertCount == 1
    }
}