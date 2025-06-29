package com.ywcheong.simple.transaction.member

import com.ywcheong.simple.transaction.exception.UserFaultException
import com.ywcheong.simple.transaction.security.jwt.JwtPayloadDto
import com.ywcheong.simple.transaction.security.jwt.JwtService
import org.seasar.doma.jdbc.UniqueConstraintException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*

interface MemberService {
    fun register(memberName: MemberName, memberPhone: MemberPhone, memberPlainPassword: MemberPlainPassword): Member
    fun publishToken(memberName: MemberName, memberPlainPassword: MemberPlainPassword): String
    fun withdraw(memberId: MemberId)
    fun find(memberId: MemberId): Member?
}

@Service
class DefaultMemberService(
    private val tokenService: JwtService,
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
) : MemberService {

    override fun register(
        memberName: MemberName, memberPhone: MemberPhone, memberPlainPassword: MemberPlainPassword
    ): Member {
        // 새로운 Member을 생성하기 위해 자동으로 채워야 하는 부분을 준비합니다.
        val memberId = MemberId(UUID.randomUUID().toString())
        val memberStatus = MemberStatus.MEMBER_REGISTERED
        val memberHashedPassword = MemberHashedPassword(passwordEncoder.encode(memberPlainPassword.value))

        val member = Member(
            id = memberId,
            name = memberName,
            phone = memberPhone,
            password = memberHashedPassword,
            status = memberStatus
        )

        // 리포지토리에 삽입합니다.
        try {
            val insertCount = memberRepository.insert(member)
            check(insertCount == 1) {
                "Member의 영속화에 실패했습니다. (값=${member})"
            }
        } catch (ex: UniqueConstraintException) {
            throw UserFaultException("이미 존재하는 사용자명을 사용할 수 없습니다.")
        }

        return member
    }

    override fun withdraw(memberId: MemberId) {
        val deleteCount = memberRepository.delete(memberId)
        if (deleteCount != 1) throw UserFaultException("이미 삭제된 사용자입니다.")
    }

    override fun find(memberId: MemberId): Member? {
        return memberRepository.selectById(memberId)
    }

    override fun publishToken(memberName: MemberName, memberPlainPassword: MemberPlainPassword): String {
        val member = memberRepository.selectByName(memberName) ?: throw UserFaultException("잘못된 사용자명 또는 비밀번호입니다.")

        // 비밀번호가 일치하는지 검사하는 로직입니다.
        if (!passwordEncoder.matches(memberPlainPassword.value, member.password.value)) {
            throw UserFaultException("잘못된 사용자명 또는 비밀번호입니다.")
        }

        val jwtPayload = JwtPayloadDto(member.id.value, member.name.value, "user")
        return tokenService.sign(jwtPayload)
    }
}