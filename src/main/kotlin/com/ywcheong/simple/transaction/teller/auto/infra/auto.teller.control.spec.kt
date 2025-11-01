package com.ywcheong.simple.transaction.teller.auto.infra

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody as OASRequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestBody

@Tag(
    name = "자동 텔러 API",
    description = "ATM 관리 기능"
)
interface AutoTellerControllerSpec {

    @Operation(
        summary = "자동 텔러 생성",
        description = "새로운 자동 텔러를 생성합니다. ID를 입력해야 합니다.",
        security = [SecurityRequirement(name = "authed-teller-admin")],
        requestBody = OASRequestBody(
            required = true,
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = CreateAutoTellerRequest::class),
                examples = [ExampleObject(
                    value = "{\"id\": \"auto001\"}"
                )]
            )]
        ),
        responses = [
            ApiResponse(
                responseCode = "201",
                description = "생성 성공"
            ),
            ApiResponse(
                responseCode = "400",
                description = "입력값 오류/중복 ID",
                content = [Content()]
            )
        ]
    )
    fun createAutoTeller(@RequestBody request: CreateAutoTellerRequest)

    @Operation(
        summary = "자동 텔러 삭제",
        description = "자동 텔러를 삭제합니다. ID를 입력해야 하며, 금고가 비어 있지 않은 자동 텔러는 삭제할 수 없습니다.",
        security = [SecurityRequirement(name = "authed-teller-admin")],
        requestBody = OASRequestBody(
            required = true,
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = DeleteAutoTellerRequest::class),
                examples = [ExampleObject(
                    value = "{\"id\": \"auto001\"}"
                )]
            )]
        ),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "삭제 성공"
            ),
            ApiResponse(
                responseCode = "400",
                description = "자동 텔러 없음 또는 잔고 비우기 필요",
                content = [Content()]
            )
        ]
    )
    fun deleteAutoTeller(@RequestBody request: DeleteAutoTellerRequest)
}
