package com.ywcheong.simple.transaction.member

import org.seasar.doma.Dao
import org.seasar.doma.Delete
import org.seasar.doma.Entity
import org.seasar.doma.Id
import org.seasar.doma.Insert
import org.seasar.doma.Select
import org.seasar.doma.Sql
import org.seasar.doma.Table
import org.seasar.doma.Update
import org.seasar.doma.boot.ConfigAutowireable

@Entity
@Table(name = "member")
class MemberEntity (
    @Id
    var id: String? = null,
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
    @Sql("""
        SELECT
            *
        FROM
            member
        WHERE
            id = /* memberId.value */'00000000-0000-0000-0000-000000000000'
    """)
    @Select
    fun findById(memberId: MemberId): MemberEntity?

    @Sql("""
        SELECT
            *
        FROM
            member
        WHERE
            name = /* memberName.value */'x'
    """)
    @Select
    fun findByName(memberName: MemberName): MemberEntity?

    @Insert
    fun insert(memberEntity: MemberEntity): Int

    @Update
    fun update(memberEntity: MemberEntity): Int

    @Sql("""
        DELETE
        FROM
            member
        WHERE
            id = /* memberId.value */'00000000-0000-0000-0000-000000000000'
    """)
    @Delete
    fun delete(memberId: MemberId): Int
}