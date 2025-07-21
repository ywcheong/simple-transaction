package com.ywcheong.simple.transaction.security.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {
    private fun setJwtContentsIntoContext(jwtToken: String) {
        val jwtTokenContents = jwtService.parse(jwtToken) ?: throw BadCredentialsException("검증할 수 없는 토큰입니다.")
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