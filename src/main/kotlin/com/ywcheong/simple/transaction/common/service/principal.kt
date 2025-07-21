package com.ywcheong.simple.transaction.common.service

import com.ywcheong.simple.transaction.member.domain.MemberId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

val klog = KotlinLogging.logger {}

@Service
class PrincipalService {
    @PreAuthorize("hasRole('USER')")
    fun getMemberId(): MemberId {
        val id = SecurityContextHolder.getContext().authentication.principal as String
        klog.info { "Member Id 식별자: $id" }
        return MemberId(id)
    }
}