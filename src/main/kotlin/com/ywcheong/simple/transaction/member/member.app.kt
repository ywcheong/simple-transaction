package com.ywcheong.simple.transaction.member

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

data class MemberRegisterRequestDto (
    val name: String,
    val password: String,
    val phone: String
)

data class MemberResponseDto (
    val id: String
) {
    constructor(member: Member) : this(member.id.value)
}

@RestController
@RequestMapping("/members")
class MemberController(private val memberService: MemberService) {
    @PostMapping
    fun register(@RequestBody requestDto: MemberRegisterRequestDto): ResponseEntity<MemberResponseDto?> {
        with(requestDto) {
            val name = MemberName(name)
            val password = MemberPlainPassword(password)
            val phone = MemberPhone(phone)

            return when (val member = memberService.register(name, phone, password)) {
                null -> ResponseEntity.status(HttpStatus.CONFLICT).body(null)
                else -> ResponseEntity.status(HttpStatus.OK).body(MemberResponseDto(member))
            }
        }
    }

    @DeleteMapping("/{id}")
    fun withdraw(@PathVariable id: String): Boolean {
        val memberId = MemberId(id)
        val result = memberService.withdraw(memberId)
        return result
    }

    @GetMapping("/{id}/tokens")
    fun jwt(@PathVariable id: String) {
        TODO()
    }
}

interface MemberService {
    fun register(name: MemberName, phone: MemberPhone, plainPassword: MemberPlainPassword): Member?
    fun withdraw(id: MemberId): Boolean
    fun findUser(id: MemberId): Member?
    fun publishJwt(id: MemberId): String?
}

@Service
class DefaultMemberService (
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder
) : MemberService {
    override fun register(name: MemberName, phone: MemberPhone, plainPassword: MemberPlainPassword): Member? {
        // Domain 오브젝트 생성
        val id = MemberId(UUID.randomUUID().toString())
        val status = MemberStatus.MEMBER_REGISTERED
        val hashedPassword = MemberHashedPassword(passwordEncoder.encode(plainPassword.value))

        // Repository에 영속화 요청
        val member = Member(id, name, phone, hashedPassword, status)
        val result = memberRepository.insert(member)

        return if (result) member else null
    }

    override fun withdraw(id: MemberId): Boolean {
        val result = memberRepository.delete(id)
        return result
    }

    override fun findUser(id: MemberId): Member? = memberRepository.selectById(id)

    override fun publishJwt(id: MemberId): String? {
        TODO("need implement")
    }

}

interface MemberRepository {
    fun selectById(memberId: MemberId): Member?
    fun insert(member: Member): Boolean
    fun delete(memberId: MemberId): Boolean
}

@Repository
class DefaultMemberRepository (
    private val memberDao: MemberDao
) : MemberRepository {
    override fun selectById(memberId: MemberId): Member? = memberDao.findById(memberId.value)?.toMember()

    override fun insert(member: Member): Boolean {
        val memberInsertEntity = MemberEntity(member)
        val insertCount: Int = memberDao.insert(memberInsertEntity)
        return (insertCount == 1)
    }

    override fun delete(memberId: MemberId): Boolean {
        val deleteCount = memberDao.delete(memberId.value)
        return (deleteCount == 1)
    }
}