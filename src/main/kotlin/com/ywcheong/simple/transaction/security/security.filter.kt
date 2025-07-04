package com.ywcheong.simple.transaction.security

import com.ywcheong.simple.transaction.security.jwt.JwtAuthFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
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
        addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
        build()
    }
}