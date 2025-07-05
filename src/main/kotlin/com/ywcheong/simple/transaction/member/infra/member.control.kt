package com.ywcheong.simple.transaction.member.infra

import com.ywcheong.simple.transaction.common.service.PrincipalService
import com.ywcheong.simple.transaction.member.domain.*
import com.ywcheong.simple.transaction.security.jwt.JwtPayloadDto
import com.ywcheong.simple.transaction.security.jwt.JwtService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
) {
    @Operation(
        summary = "회원 가입",
        description = "새로운 사용자를 등록합니다. 아이디, 이름, 비밀번호, 전화번호를 입력해야 합니다.",
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = RegisterRequest::class),
                examples = [ExampleObject(
                    value = "{\"id\": \"testuser\", \"name\": \"홍길동\", \"password\": \"P@ssw0rd!\", \"phone\": \"+82-10-1234-5678\"}"
                )]
            )]
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "회원가입 성공",
                content = [Content(schema = Schema(implementation = RegisterResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "입력값 오류/중복 아이디"),
        ]
    )
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
            memberRepository.insert(member)
        } catch (ex: DuplicateKeyException) {
            throw DuplicateMemberIdException()
        }

        // 응답 반환
        ResponseEntity.status(HttpStatus.OK).body(RegisterResponse(memberId.value))
    }

    @Operation(
        summary = "회원 탈퇴",
        description = "현재 인증된 사용자가 회원 탈퇴를 요청합니다.",
        security = [SecurityRequirement(name = "bearer-key")],
        responses = [ApiResponse(
            responseCode = "200",
            description = "탈퇴 성공",
            content = [Content(schema = Schema(implementation = WithdrawResponse::class))]
        ), ApiResponse(responseCode = "400", description = "이미 탈퇴된 계정"), ApiResponse(
            responseCode = "403", description = "인증 실패"
        )]
    )
    @DeleteMapping
    fun withdrawExistingMember(): ResponseEntity<WithdrawResponse> {
        // 요청 준비
        val memberId = principalService.getMemberId()

        // 의존성 조율
        val success = memberRepository.delete(memberId)
        if (!success) throw DeletedMemberException()

        // 응답 반환
        val responseDto = WithdrawResponse(memberId.value)
        return ResponseEntity.status(HttpStatus.OK).body(responseDto)
    }

    @Operation(
        summary = "JWT 토큰 발급",
        description = "아이디와 비밀번호를 입력받아 인증 토큰(JWT)을 발급합니다.",
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = TokenRequest::class),
                examples = [ExampleObject(
                    value = "{\"id\": \"testuser\", \"password\": \"P@ssw0rd!\"}"
                )]
            )]
        ),
        responses = [ApiResponse(
            responseCode = "200",
            description = "토큰 발급 성공",
            content = [Content(schema = Schema(implementation = TokenResponse::class))]
        ), ApiResponse(responseCode = "400", description = "아이디/비밀번호 오류")]
    )
    @PostMapping("/tokens")
    fun publishAuthToken(@RequestBody request: TokenRequest): ResponseEntity<TokenResponse> = with(request) {
        // 요청 준비
        val memberId = MemberId(id)
        val claimedPlainPassword = MemberPlainPassword(password)

        // 의존성 조율
        val member = memberRepository.findById(memberId) ?: throw MemberLoginException()
        val storedHashedPassword = member.password
        if (!memberPasswordHashService.isEqual(claimedPlainPassword, storedHashedPassword)) throw MemberLoginException()
        val jwtToken = jwtService.sign(
            JwtPayloadDto(
                sub = member.id.value, name = member.name.value, role = "ROLE_USER"
            )
        )

        // 응답 반환
        ResponseEntity.status(HttpStatus.OK).body(TokenResponse(jwtToken))
    }
}