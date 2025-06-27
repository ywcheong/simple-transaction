package com.ywcheong.simple.transaction.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import io.jsonwebtoken.JwtException
import javax.crypto.SecretKey

@Component
class JwtAuthFilter() : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain
    ) {
        TODO("Not yet implemented")
    }

}

// DTO representing the JWT payload
data class JwtPayloadDto(
    val sub: String, val name: String, val role: String
)

// Service for signing and verifying JWTs
class JwtService(secretKey: String) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secretKey.toByteArray())

    // Create a compact JWT from JwtPayloadDto
    fun sign(payload: JwtPayloadDto): String {
        return Jwts.builder().claim("name", payload.name).claim("role", payload.role).subject(payload.sub).signWith(key)
            .compact()
    }

    // Parse a compact JWT string to JwtPayloadDto, or return null if invalid
    fun parse(token: String): JwtPayloadDto? {
        return try {
            val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload

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
