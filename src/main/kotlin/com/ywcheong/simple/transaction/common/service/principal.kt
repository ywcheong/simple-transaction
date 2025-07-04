package com.ywcheong.simple.transaction.common.service

import com.ywcheong.simple.transaction.member.domain.MemberId
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class PrincipalService {
    fun getMemberId(): MemberId {
        val id = SecurityContextHolder.getContext().authentication.principal as String
        return MemberId(id)
    }
}