package com.ywcheong.simple.transaction.security.jwt

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Component

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

