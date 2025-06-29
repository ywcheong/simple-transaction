package com.ywcheong.simple.transaction.member

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

data class MemberRegisterRequestDto(
    val name: String, val password: String, val phone: String
)

data class MemberRegisterResponseDto(
    val id: String
)

//data class MemberWithdrawRequestDto()

data class MemberWithdrawResponseDto(
    val success: Boolean = true
)

data class MemberTokenRequestDto(
    val name: String, val password: String
)

data class MemberTokenResponseDto(val token: String)

@RestController
@RequestMapping("/members")
class MemberController(
    private val memberService: MemberService
) {
    @PostMapping
    fun register(@RequestBody registerDto: MemberRegisterRequestDto): ResponseEntity<MemberRegisterResponseDto> =
        with(registerDto) {
            // 서비스 요청 준비
            val memberName = MemberName(name)
            val memberPlainPassword = MemberPlainPassword(password)
            val memberPhone = MemberPhone(phone)

            // 서비스 요청 호출
            val registeredMember: Member = memberService.register(
                memberName = memberName,
                memberPhone = memberPhone,
                memberPlainPassword = memberPlainPassword,
            )

            // 서비스 응답 준비
            val responseDto = MemberRegisterResponseDto(
                id = registeredMember.id.value
            )

            // 서비스 응답 반환
            ResponseEntity.status(HttpStatus.OK).body(responseDto)
        }

    @DeleteMapping
    fun withdraw(): ResponseEntity<MemberWithdrawResponseDto> {
        // 서비스 요청 준비
        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated) {
            throw AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.")
        }

        val memberId = MemberId(auth.principal as String)

        // 서비스 요청 호출
        memberService.withdraw(
            memberId = memberId
        )

        // 서비스 응답 준비
        val responseDto = MemberWithdrawResponseDto()

        // 서비스 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(responseDto)
    }

    @PostMapping("/tokens")
    fun tokens(@RequestBody requestDto: MemberTokenRequestDto): ResponseEntity<MemberTokenResponseDto> =
        with(requestDto) {
            // 서비스 요청 준비
            val memberName = MemberName(name)
            val memberPlainPassword = MemberPlainPassword(password)

            // 서비스 요청 호출
            val tokenString: String = memberService.publishToken(
                memberName = memberName, memberPlainPassword = memberPlainPassword
            )

            // 서비스 응답 준비
            val responseDto = MemberTokenResponseDto(
                token = tokenString
            )

            // 서비스 응답 반화
            ResponseEntity.status(HttpStatus.OK).body(responseDto)
        }
}