package com.ywcheong.simple.transaction.common.config

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.context.annotation.Configuration

@Configuration
@SecurityScheme(
    name = "authed-member",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    `in` = SecuritySchemeIn.HEADER,
    description = "`/members/tokens` 엔드포인트에서 로그인 후 발급받은 토큰을 입력하세요. 이 토큰은 HTTP 요청 과정에서 `Authorization: Bearer {{token}}`으로 첨부됩니다."
)
class OpenApiConfig