package com.ywcheong.simple.transaction.common.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.net.URI

val logger_ = KotlinLogging.logger { }

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    /* 4xx : 도메인 규칙 위반, 사용자의 실수 */
    @ExceptionHandler(UserFaultException::class)
    fun handleUserFault(ex: UserFaultException, request: WebRequest): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "Bad request").apply {
            title = "User error"
            type = URI("/help/ui")
            instance = URI(request.getDescription(false).removePrefix("uri="))
        }

    /* 403 : 권한 없는 엔드포인트로 접근 */
    @ExceptionHandler(AuthorizationDeniedException::class)
    fun handleMethodAuthorizationDenied(ex: AuthorizationDeniedException, request: WebRequest): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.message ?: "Forbidden").apply {
            title = "Forbidden"
            type = URI("/help/ui")
            instance = URI(request.getDescription(false).removePrefix("uri="))
        }

    /* 5xx: 기타 모든 오류는 서버 오류로 간주 */
    @ExceptionHandler(Throwable::class)
    fun handleUnexpected(ex: Throwable, request: WebRequest): ProblemDetail {
        logger_.error { "Unexpected error: $ex" }
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"
        ).apply {
            title = "Internal Server Error"
            type = URI("/help/ui")
            instance = URI(request.getDescription(false).removePrefix("uri="))
        }
    }
}