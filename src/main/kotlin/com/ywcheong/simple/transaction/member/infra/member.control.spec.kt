package com.ywcheong.simple.transaction.member.infra

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "회원 API", description = "회원 관련 기능")
interface MemberControllerSpec {
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
            ApiResponse(responseCode = "400", description = "입력값 오류/중복 아이디", content = [Content()]),
        ]
    )
    fun registerNewMember(@RequestBody request: RegisterRequest): ResponseEntity<RegisterResponse>

    @Operation(
        summary = "회원 탈퇴",
        description = "현재 인증된 사용자가 회원 탈퇴를 요청합니다.",
        security = [SecurityRequirement(name = "bearer-key")],
        responses = [ApiResponse(
            responseCode = "200",
            description = "탈퇴 성공",
            content = [Content(schema = Schema(implementation = WithdrawResponse::class))]
        ), ApiResponse(responseCode = "400", description = "이미 탈퇴된 계정", content = [Content()]), ApiResponse(
            responseCode = "403", description = "인증 실패", content = [Content()]
        )]
    )
    fun withdrawExistingMember(): ResponseEntity<WithdrawResponse>

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
        ), ApiResponse(responseCode = "400", description = "아이디/비밀번호 오류", content = [Content()])]
    )
    fun publishAuthToken(@RequestBody request: TokenRequest): ResponseEntity<TokenResponse>
}