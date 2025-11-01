package com.ywcheong.simple.transaction.teller.base.infra

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody as SpringRequestBody

@Tag(
    name = "텔러 관리 API", description = "텔러 권한 및 서명키 발급/검증 기능"
)
interface TellerControllerSpec {

    /* ----------------------------  권한 조회  ---------------------------- */

    @Operation(
        summary = "텔러 권한 조회",
        description = "지정한 텔러(ID)의 보유 권한 목록을 반환합니다.",
        security = [SecurityRequirement(name = "authed-teller-admin")],
        responses = [ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(schema = Schema(implementation = LookupTellerPermissionResponse::class))]
        ), ApiResponse(
            responseCode = "400", description = "잘못된 텔러 ID, 존재하지 않는 텔러 등", content = [Content()]
        )]
    )
    fun lookupEmployeeTellerPermissions(
        @PathVariable tellerId: String
    ): LookupTellerPermissionResponse


    /* ----------------------------  권한 부여  ---------------------------- */

    @Operation(
        summary = "텔러 권한 부여",
        description = "지정한 텔러에게 새로운 권한을 부여합니다.",
        security = [SecurityRequirement(name = "authed-teller-admin")],
        responses = [ApiResponse(
            responseCode = "200",
            description = "부여 성공",
            content = [Content(schema = Schema(implementation = GrantTellerPermissionResponse::class))]
        ), ApiResponse(
            responseCode = "400", description = "잘못된 요청(존재하지 않는 텔러 등)", content = [Content()]
        )]
    )
    fun grantEmployeeTellerPermission(
        @PathVariable tellerId: String, @PathVariable grantingPermission: String
    ): GrantTellerPermissionResponse


    /* ----------------------------  권한 철회  ---------------------------- */

    @Operation(
        summary = "텔러 권한 제거",
        description = "텔러에게서 특정 권한을 제거합니다.",
        security = [SecurityRequirement(name = "authed-teller-admin")],
        responses = [ApiResponse(
            responseCode = "200",
            description = "철회 성공",
            content = [Content(schema = Schema(implementation = RevokeTellerPermissionResponse::class))]
        ), ApiResponse(
            responseCode = "400", description = "잘못된 요청(존재하지 않는 텔러 등)", content = [Content()]
        )]
    )
    fun revokeEmployeeTellerPermission(
        @PathVariable tellerId: String, @PathVariable revokingPermission: String
    ): RevokeTellerPermissionResponse


    /* ----------------------------  서명키 발급  ---------------------------- */

    @Operation(
        summary = "서명용 비대칭키 발급",
        description = "지정한 텔러에게 서명용 비대칭키를 발급합니다. 이미 키가 존재하면 실패합니다.",
        security = [SecurityRequirement(name = "authed-teller-admin")],
        responses = [ApiResponse(
            responseCode = "201",
            description = "발급 성공",
            content = [Content(schema = Schema(implementation = CreateSigningKeyResponse::class))]
        ), ApiResponse(
            responseCode = "400", description = "이미 키가 존재하거나 잘못된 텔러 ID", content = [Content()]
        )]
    )
    fun createSigningKey(
        @PathVariable tellerId: String
    ): CreateSigningKeyResponse


    /* ----------------------------  서명키 무효화  ---------------------------- */

    @Operation(
        summary = "서명용 비대칭키 무효화",
        description = "지정한 텔러의 서명용 공개키를 무효화(삭제)합니다.",
        security = [SecurityRequirement(name = "authed-teller-admin")],
        responses = [ApiResponse(
            responseCode = "200", description = "무효화 성공", content = [Content()]
        ), ApiResponse(
            responseCode = "400", description = "키가 존재하지 않거나 잘못된 텔러 ID", content = [Content()]
        )]
    )
    fun invalidateSigningKey(
        @PathVariable tellerId: String
    )


    /* ----------------------------  서명 검증  ---------------------------- */

    @Operation(
        summary = "텔러 서명 검증",
        description = "텔러가 생성한 서명(전자서명)을 검증합니다.",
        security = [SecurityRequirement(name = "authed-teller-admin")],
        requestBody = RequestBody(
            required = true, content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = VerifySignRequest::class),
                examples = [ExampleObject(
                    value = """
                        {
                          "checkingPermission": "MEMBER_DEPOSIT_CERTIFICATION",
                          "signedMessage"   : "BASE64_SIGNATURE:TEXT_MESSAGE"
                        }
                    """
                )]
            )]
        ),
        responses = [ApiResponse(
            responseCode = "200",
            description = "검증 성공 – 원본 메시지 반환",
            content = [Content(schema = Schema(implementation = VerifySignResponse::class))]
        ), ApiResponse(
            responseCode = "400", description = "검증 실패(권한 부족, 잘못된 서명, 키 없음 등)", content = [Content()]
        )]
    )
    fun verifySign(
        @PathVariable signedTellerId: String, @SpringRequestBody request: VerifySignRequest
    ): VerifySignResponse
}
