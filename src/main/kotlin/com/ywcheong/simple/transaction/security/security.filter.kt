package com.ywcheong.simple.transaction.security

import com.ywcheong.simple.transaction.security.jwt.JwtAuthFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.stereotype.Component

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val unauthorizedHandlerEntryPoint: UnauthorizedHandlerEntryPoint,
    private val jwtFilter: JwtAuthFilter
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain = http.run {
        csrf { it.disable() }
        sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        authorizeHttpRequests {
            it.requestMatchers(HttpMethod.POST, "/members").permitAll()
                .requestMatchers("/members/tokens", "/help/**")
                .permitAll().anyRequest().authenticated()
        }
        exceptionHandling { it.authenticationEntryPoint(unauthorizedHandlerEntryPoint) }
        addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
        build()
    }
}

@Component
class UnauthorizedHandlerEntryPoint : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: org.springframework.security.core.AuthenticationException
    ) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
    }
}