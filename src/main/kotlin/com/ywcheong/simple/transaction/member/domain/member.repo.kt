package com.ywcheong.simple.transaction.member.domain

interface MemberRepository {
    fun findById(memberId: MemberId): Member?
    fun delete(memberId: MemberId): Boolean
    fun insert(member: Member): Boolean
}