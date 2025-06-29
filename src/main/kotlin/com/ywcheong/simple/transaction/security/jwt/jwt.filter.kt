package com.ywcheong.simple.transaction.security.jwt

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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
            val payload = jwtService.parse(token)
            if (payload != null) {
                // You can add more authorities based on your payload.role if needed
                val authorities = listOf(SimpleGrantedAuthority(payload.role))
                val authentication = UsernamePasswordAuthenticationToken(
                    payload.sub, null, authorities
                )
                SecurityContextHolder.getContext().authentication = authentication
            }
        }
        // Always continue the filter chain
        filterChain.doFilter(request, response)
    }
}


// DTO representing the JWT payload
data class JwtPayloadDto(
    val sub: String, val name: String, val role: String
)

// Service for signing and verifying JWTs using asymmetric keys
@Component
class JwtService(
    private val jwtKeyProvider: JwtKeyProvider // Inject the key provider
) {
    // Create a compact JWT from JwtPayloadDto
    fun sign(payload: JwtPayloadDto): String {
        return Jwts.builder().claim("name", payload.name).claim("role", payload.role).subject(payload.sub)
            .signWith(jwtKeyProvider.privateKey) // Use private key for signing
            .compact()
    }

    // Parse a compact JWT string to JwtPayloadDto, or return null if invalid
    fun parse(token: String): JwtPayloadDto? {
        return try {
            val claims = Jwts.parser().verifyWith(jwtKeyProvider.publicKey) // Use public key for verification
                .build().parseSignedClaims(token).payload

            val sub = claims.subject ?: return null
            val name = claims["name"] as? String ?: return null
            val role = claims["role"] as? String ?: return null

            JwtPayloadDto(sub, name, role)
        } catch (e: JwtException) {
            null // Signature invalid or token malformed
        } catch (e: Exception) {
            null // Claims missing or wrong type
        }
    }
}

