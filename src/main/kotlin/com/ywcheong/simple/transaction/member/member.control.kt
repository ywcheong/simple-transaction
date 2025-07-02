package com.ywcheong.simple.transaction.member

import com.ywcheong.simple.transaction.exception.UserFaultException
import com.ywcheong.simple.transaction.exception.check_domain
import com.ywcheong.simple.transaction.exception.logger_
import com.ywcheong.simple.transaction.security.jwt.JwtPayloadDto
import com.ywcheong.simple.transaction.security.jwt.JwtService
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

data class RegisterRequest(val id: String, val name: String, val password: String, val phone: String)
data class RegisterResponse(val id: String)

data class WithdrawResponse(val id: String)

data class TokenRequest(val id: String, val password: String)
data class TokenResponse(val token: String)

@RestController
@RequestMapping("/members")
class MemberController(
    private val memberPasswordHashService: MemberPasswordHashService,
    private val memberDao: MemberDao,
    private val jwtService: JwtService
) {
    @PostMapping
    fun registerNewMember(@RequestBody request: RegisterRequest): ResponseEntity<RegisterResponse> = with(request) {
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
            val memberEntity = MemberEntity(member)
            val insertCount = memberDao.insert(memberEntity)
            check(insertCount == 1) { "회원 DB에 삽입이 정상적으로 완료되지 않았습니다. (삽입 카운트: $insertCount)" }
        } catch (ex: DuplicateKeyException) {
            throw UserFaultException("이미 존재하는 회원 ID입니다.")
        }

        // 응답 반환
        ResponseEntity.status(HttpStatus.OK).body(RegisterResponse(memberId.value))
    }

    @DeleteMapping
    fun withdrawExistingMember(): ResponseEntity<WithdrawResponse> {
        // 요청 준비
        val id = SecurityContextHolder.getContext().authentication.principal as String
        logger_.info { "Context ID: $id" }
        val memberId = MemberId(id)

        // 의존성 조율
        val deleteCount = memberDao.delete(memberId)
        check_domain(deleteCount == 1) { "이미 삭제된 회원입니다." }

        // 응답 반환
        val responseDto = WithdrawResponse(memberId.value)
        return ResponseEntity.status(HttpStatus.OK).body(responseDto)
    }

    @PostMapping("/tokens")
    fun publishAuthToken(@RequestBody request: TokenRequest): ResponseEntity<TokenResponse> = with(request) {
        // 요청 준비
        val memberId = MemberId(id)
        val claimedPlainPassword = MemberPlainPassword(password)

        // 의존성 조율
        val member = memberDao.findById(memberId)?.toMember() ?: throw UserFaultException("존재하지 않는 회원입니다.")
        val storedHashedPassword = member.password
        if (!memberPasswordHashService.isEqual(
                claimedPlainPassword, storedHashedPassword
            )
        ) throw UserFaultException("비밀번호가 일치하지 않습니다.")
        val jwtToken = jwtService.sign(
            JwtPayloadDto(
                sub = member.id.value, name = member.name.value, role = "ROLE_USER"
            )
        )

        // 응답 반환
        ResponseEntity.status(HttpStatus.OK).body(TokenResponse(jwtToken))
    }
}