package com.ywcheong.simple.transaction.security.jwt

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.UnsupportedJwtException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

// DTO representing the JWT payload
data class JwtTokenContents(
    val sub: String,
    val exp: Date,
    val authorities: List<SimpleGrantedAuthority>,
)

// Service for signing and verifying JWTs using asymmetric keys
@Component
class JwtService(
    private val jwtKeyProvider: JwtKeyProvider // Inject the key provider
) {
    fun oneDayAfterNow(): Date = Date.from(Instant.now().plus(1, ChronoUnit.DAYS))

    fun sign(sub: String, authorities: List<SimpleGrantedAuthority>): String =
        Jwts.builder().subject(sub).expiration(oneDayAfterNow()).claim("authorities", authorities.map { it.authority })
            .signWith(jwtKeyProvider.privateKey).compact()

    fun parseAuthorities(authClaim: Any?): List<SimpleGrantedAuthority>? =
        (authClaim as? List<*>)?.map { SimpleGrantedAuthority(it as? String) }

    fun parse(token: String): JwtTokenContents? {
        val claims = try {
            Jwts.parser().verifyWith(jwtKeyProvider.publicKey).build().parseSignedClaims(token).payload
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: UnsupportedJwtException) {
            null
        } catch (_: JwtException) {
            null
        } ?: return null

        val sub = claims.subject ?: return null
        val exp = claims.expiration ?: return null
        val authorities = parseAuthorities(claims["authorities"]) ?: return null

        return JwtTokenContents(sub, exp, authorities)
    }
}

