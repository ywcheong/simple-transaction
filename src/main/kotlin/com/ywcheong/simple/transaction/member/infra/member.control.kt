package com.ywcheong.simple.transaction.member.infra

import com.ywcheong.simple.transaction.common.service.PrincipalService
import com.ywcheong.simple.transaction.member.domain.*
import com.ywcheong.simple.transaction.security.jwt.JwtService
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.bind.annotation.*

data class RegisterRequest(val id: String, val name: String, val password: String, val phone: String)
data class RegisterResponse(val id: String)

data class WithdrawResponse(val id: String)

data class TokenRequest(val id: String, val password: String)
data class TokenResponse(val token: String)

@RestController
@RequestMapping("/members")
class MemberController(
    private val jwtService: JwtService,
    private val memberPasswordHashService: MemberPasswordHashService,
    private val principalService: PrincipalService,
    private val memberRepository: MemberRepository
) : MemberControllerSpec {
    @PostMapping
    override fun registerNewMember(@RequestBody request: RegisterRequest): ResponseEntity<RegisterResponse> =
        with(request) {
            // 요청 준비
            val memberId = MemberId(id)
            val memberName = MemberName(name)
            val memberPlainPassword = MemberPlainPassword(password)
            val memberPhone = MemberPhone(phone)
            val memberStatus = MemberStatus.MEMBER_REGISTERED

            // 의존성 조율
            val memberHashedPassword = memberPasswordHashService.encode(memberPlainPassword)
            val member = Member(
                id = memberId,
                name = memberName,
                phone = memberPhone,
                password = memberHashedPassword,
                status = memberStatus
            )
            try {
                memberRepository.insert(member)
            } catch (_: DuplicateKeyException) {
                throw DuplicateMemberIdException()
            }

            // 응답 반환
            ResponseEntity.status(HttpStatus.OK).body(RegisterResponse(memberId.value))
        }

    @DeleteMapping
    override fun withdrawExistingMember(): ResponseEntity<WithdrawResponse> {
        // 요청 준비
        val memberId = principalService.getMemberId()

        // 의존성 조율
        val success = memberRepository.delete(memberId)
        if (!success) throw DeletedMemberException()

        // 응답 반환
        val responseDto = WithdrawResponse(memberId.value)
        return ResponseEntity.status(HttpStatus.OK).body(responseDto)
    }


    @PostMapping("/tokens")
    override fun publishAuthToken(@RequestBody request: TokenRequest): ResponseEntity<TokenResponse> = with(request) {
        // 요청 준비
        val memberId = MemberId(id)
        val claimedPlainPassword = MemberPlainPassword(password)

        // 의존성 조율
        val member = memberRepository.findById(memberId) ?: throw MemberLoginException()
        val storedHashedPassword = member.password
        if (!memberPasswordHashService.isEqual(claimedPlainPassword, storedHashedPassword)) throw MemberLoginException()
        val jwtToken = jwtService.sign(member.id.value, listOf(SimpleGrantedAuthority("ROLE_USER")))

        // 응답 반환
        ResponseEntity.status(HttpStatus.OK).body(TokenResponse(jwtToken))
    }
}