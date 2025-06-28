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
@Table(name = "Member")
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

    fun toMember(): Member {
        require(id != null && name != null && phone != null && password != null && status != null) {
            "Member의 필드가 Not-null임에도, 데이터베이스에서 Null인 열을 조회했습니다. (값=${this.toString()})"
        }

        val memberStatus = MemberStatus.fromValue(status!!)
        require(memberStatus != null) {
            "데이터베이스에서 검색된 Member의 status의 값이 정상 범위를 이탈했습니다. (값=${this.toString()})"
        }

        return Member(
            id = MemberId(id!!),
            name = MemberName(name!!),
            phone = MemberPhone(phone!!),
            password = MemberHashedPassword(password!!),
            status = memberStatus,
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
            Member
        WHERE
            id = /* id */'00000000-0000-0000-0000-000000000000'
    """)
    @Select
    fun findById(id: String): MemberEntity?

    @Insert
    fun insert(member: MemberEntity): Int

    @Update
    fun update(member: MemberEntity): Int

    @Sql("""
        DELETE
        FROM
            Member
        WHERE
            id = /* id */'00000000-0000-0000-0000-000000000000'
    """)
    @Delete
    fun delete(id: String): Int
}