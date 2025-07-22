package com.ywcheong.simple.transaction.security.jwt

import com.ywcheong.simple.transaction.common.service.TimeService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService, private val timeService: TimeService
) : OncePerRequestFilter() {
    private fun setJwtContentsIntoContext(jwtToken: String) {
        val jwtTokenContents =
            jwtService.parse(jwtToken) ?: throw BadCredentialsException("인증 토큰이 손상되어 인증할 수 없습니다. 다시 로그인해 주세요.")
        if (jwtTokenContents.isExpiredAt(timeService.nowDate())) throw CredentialsExpiredException("인증 토큰이 오래되어 인증할 수 없습니다. 다시 로그인해 주세요.")
        val authentication =
            PreAuthenticatedAuthenticationToken(jwtTokenContents.sub, jwtToken, jwtTokenContents.authorities)
        SecurityContextHolder.getContext().authentication = authentication
    }

    override fun doFilterInternal(
        request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        if (authHeader?.startsWith("Bearer ") == true) {
            val jwtToken = authHeader.substring(7)
            setJwtContentsIntoContext(jwtToken)
        }

        filterChain.doFilter(request, response)
    }
}