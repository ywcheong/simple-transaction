package com.ywcheong.simple.transaction.security.member

import com.ywcheong.simple.transaction.member.MemberHashedPassword
import com.ywcheong.simple.transaction.member.MemberPasswordHashService
import com.ywcheong.simple.transaction.member.MemberPlainPassword
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Configuration
class PasswordEncoderConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder()
    }
}

@Service
class DefaultMemberPasswordHashService(
    private val passwordEncoder: PasswordEncoder
): MemberPasswordHashService {
    override fun encode(plainPassword: MemberPlainPassword): MemberHashedPassword {
        val plainString: String = plainPassword.value
        val hashedString: String = passwordEncoder.encode(plainString)
        return MemberHashedPassword(hashedString)
    }

    override fun isEqual(plainPassword: MemberPlainPassword, hashedPassword: MemberHashedPassword): Boolean {
        val plainString = plainPassword.value
        val hashedString = hashedPassword.value
        return passwordEncoder.matches(plainString, hashedString)
    }
}