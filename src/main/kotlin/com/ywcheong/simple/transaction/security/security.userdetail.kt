package com.ywcheong.simple.transaction.security

import com.ywcheong.simple.transaction.member.Member
import com.ywcheong.simple.transaction.member.MemberDao
import com.ywcheong.simple.transaction.member.MemberId
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

// Spring Security를 위해 구현한, UserDetails & UserDetailsService 인터페이스
// Spring Security의 `DaoAuthenticationProvider` Bean이 이 인터페이스에 의존해서 사용자 정보의 유효성을 판정함
class CustomUserDetails(val member: Member) : UserDetails {
    override fun getAuthorities(): MutableCollection<out GrantedAuthority> =
        mutableListOf(SimpleGrantedAuthority("user"))

    override fun getPassword(): String = member.password.value
    override fun getUsername(): String = member.name.value
}

@Service
class CustomUserDetailsService(
    private val memberDao: MemberDao
) : UserDetailsService {
    override fun loadUserByUsername(username: String?): UserDetails {
        if (username == null) throw UsernameNotFoundException("회원 ID가 비었습니다.")
        val member =
            memberDao.findById(MemberId(username))?.toMember() ?: throw UsernameNotFoundException("회원 ID를 찾을 수 없습니다.")
        return CustomUserDetails(member)
    }
}