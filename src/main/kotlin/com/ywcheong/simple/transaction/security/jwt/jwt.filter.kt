package com.ywcheong.simple.transaction.security.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        val token = if (authHeader?.startsWith("Bearer ") == true) {
            authHeader.substring(7)
        } else {
            null
        }

        // If token is present and valid, set authentication in the context
        if (!token.isNullOrBlank()) {
            val payload = jwtService.parse(token) ?: throw BadCredentialsException("검증할 수 없는 토큰입니다.")
            val authorities = listOf(SimpleGrantedAuthority(payload.role))
            val authentication = UsernamePasswordAuthenticationToken(
                payload.sub, null, authorities
            )
            SecurityContextHolder.getContext().authentication = authentication
        }
        // Always continue the filter chain
        filterChain.doFilter(request, response)
    }
}