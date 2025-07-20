package com.ywcheong.simple.transaction.account.infra

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "계좌 API", description = "계좌 관련 기능")
interface AccountControllerSpec {

    @Operation(
        summary = "계좌 목록 조회",
        description = "현재 인증된 사용자가 소유한 모든 계좌의 ID 목록을 반환합니다.",
        security = [SecurityRequirement(name = "bearer-key")],
        responses = [ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(schema = Schema(implementation = LookupEveryAccountsResponse::class))]
        )]
    )
    fun lookupEveryAccounts(): ResponseEntity<LookupEveryAccountsResponse>

    @Operation(
        summary = "계좌 상세 조회",
        description = "계좌 ID로 단일 계좌의 잔액과 보류 잔액을 조회합니다. 본인 소유 계좌만 조회 가능합니다.",
        security = [SecurityRequirement(name = "bearer-key")],
        parameters = [Parameter(name = "id", description = "계좌 ID (UUID4)", required = true, `in` = ParameterIn.PATH)],
        responses = [ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(schema = Schema(implementation = LookupOneAccountResponse::class))]
        ), ApiResponse(responseCode = "400", description = "잘못된 계좌 ID 또는 권한 없음", content = [Content()])]
    )
    fun lookupOneAccount(@PathVariable id: String): ResponseEntity<LookupOneAccountResponse>

    @Operation(
        summary = "계좌 개설",
        description = "새로운 계좌를 개설합니다. 반환값으로 개설된 계좌 ID를 제공합니다.",
        security = [SecurityRequirement(name = "bearer-key")],
        responses = [ApiResponse(
            responseCode = "200",
            description = "개설 성공",
            content = [Content(schema = Schema(implementation = OpenAccountResponse::class))]
        )]
    )
    fun openNewAccount(): ResponseEntity<OpenAccountResponse>

    @Operation(
        summary = "계좌 폐쇄",
        description = "계좌를 폐쇄(비활성화)합니다. 잔고가 0이어야만 폐쇄 가능합니다.",
        security = [SecurityRequirement(name = "bearer-key")],
        parameters = [Parameter(name = "id", description = "계좌 ID (UUID4)", required = true, `in` = ParameterIn.PATH)],
        responses = [ApiResponse(responseCode = "200", description = "폐쇄 성공", content = [Content()]), ApiResponse(
            responseCode = "400", description = "잔고가 남아 있거나 권한 없음", content = [Content()]
        )]
    )
    fun closeExistingAccount(@PathVariable id: String): ResponseEntity<Nothing>

    @Operation(
        summary = "계좌 입금",
        description = "지정한 계좌에 금액을 입금합니다.",
        security = [SecurityRequirement(name = "bearer-key")],
        parameters = [Parameter(name = "id", description = "계좌 ID (UUID4)", required = true, `in` = ParameterIn.PATH)],
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = DepositRequest::class),
                examples = [ExampleObject(value = "{\"amount\": 1000}")]
            )]
        ),
        responses = [ApiResponse(
            responseCode = "200",
            description = "입금 성공",
            content = [Content(schema = Schema(implementation = DepositResponse::class))]
        ), ApiResponse(
            responseCode = "400", description = "잘못된 요청(음수/0 금액, 권한 없음 등)", content = [Content()]
        )]
    )
    fun executeDeposit(
        @PathVariable id: String, @RequestBody request: DepositRequest
    ): ResponseEntity<DepositResponse>

    @Operation(
        summary = "계좌 출금",
        description = "지정한 계좌에서 금액을 출금합니다.",
        security = [SecurityRequirement(name = "bearer-key")],
        parameters = [Parameter(name = "id", description = "계좌 ID (UUID4)", required = true, `in` = ParameterIn.PATH)],
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = WithdrawRequest::class),
                examples = [ExampleObject(value = "{\"amount\": 500}")]
            )]
        ),
        responses = [ApiResponse(
            responseCode = "200",
            description = "출금 성공",
            content = [Content(schema = Schema(implementation = WithdrawResponse::class))]
        ), ApiResponse(
            responseCode = "400", description = "잘못된 요청(음수/0 금액, 잔고 부족, 권한 없음 등)", content = [Content()]
        )]
    )
    fun executeWithdraw(
        @PathVariable id: String, @RequestBody request: WithdrawRequest
    ): ResponseEntity<WithdrawResponse>
}

@Tag(name = "송금 API", description = "송금 관련 기능")
interface TransferControllerSpec {
    @Operation(
        summary = "계좌 송금",
        description = "계좌 간 송금을 실행합니다. 고액 송금(타인에게 100만원 이상)은 대기 상태가 됩니다.",
        security = [SecurityRequirement(name = "bearer-key")],
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = TransferRequest::class),
                examples = [ExampleObject(value = "{\"from\": \"계좌ID1\", \"to\": \"계좌ID2\", \"amount\": 1000000}")]
            )]
        ),
        responses = [ApiResponse(
            responseCode = "200", description = "즉시 송금 성공", content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = TransferResponse::class),
                examples = [ExampleObject(
                    name = "즉시송금예시",
                    summary = "즉시 송금 성공 예시",
                    value = "{\"eventId\": \"1142d4f9-279e-4828-9662-d8382923449b\", \"pending\": false}"
                )]
            )]
        ), ApiResponse(
            responseCode = "202", description = "고액 송금 대기", content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = TransferResponse::class),
                examples = [ExampleObject(
                    name = "고액송금예시",
                    summary = "고액 송금 대기 예시",
                    value = "{\"eventId\": \"1142d4f9-279e-4828-9662-d8382923449b\", \"pending\": true}"
                )]
            )]
        ), ApiResponse(
            responseCode = "400", description = "잘못된 요청(음수/0 금액, 잔고 부족, 권한 없음 등)", content = [Content()]
        )]
    )
    fun executeTransfer(@RequestBody request: TransferRequest): ResponseEntity<TransferResponse>

    @Operation(
        summary = "송금 처리 상태 확인",
        description = "송금 이벤트 ID로 송금의 처리 상태(대기/완료/거부)를 조회합니다.",
        security = [SecurityRequirement(name = "bearer-key")],
        parameters = [Parameter(
            name = "id", description = "송금 이벤트 ID (UUID4)", required = true, `in` = ParameterIn.PATH
        )],
        responses = [ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(schema = Schema(implementation = CheckTransferResponse::class))]
        )]
    )
    fun checkTransfer(@PathVariable id: String): ResponseEntity<CheckTransferResponse>
}