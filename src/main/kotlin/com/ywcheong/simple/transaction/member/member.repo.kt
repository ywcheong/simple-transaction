package com.ywcheong.simple.transaction.member

import org.springframework.stereotype.Repository

interface MemberRepository {
    fun selectById(memberId: MemberId): Member?
    fun selectByName(memberName: MemberName): Member?

    fun insert(member: Member): Int
    fun delete(memberId: MemberId): Int
}

@Repository
class DefaultMemberRepository(
    private val memberDao: MemberDao
) : MemberRepository {
    override fun selectById(memberId: MemberId): Member? {
        return memberDao.findById(memberId.value)?.toMember()
    }

    override fun selectByName(memberName: MemberName): Member? {
        return memberDao.findByName(memberName.value)?.toMember()
    }

    override fun insert(member: Member): Int {
        val memberInsertEntity = MemberEntity(member)
        val insertCount: Int = memberDao.insert(memberInsertEntity)
        return insertCount
    }

    override fun delete(memberId: MemberId) = memberDao.delete(memberId.value)
}