package com.ywcheong.simple.transaction.member.domain

import com.ywcheong.simple.transaction.exception.check_domain

data class MemberId(val value: String) {
    init {
        check_domain(value.length in MIN_LENGTH..MAX_LENGTH) { "회원 ID는 ${MIN_LENGTH}자 이상 ${MAX_LENGTH}자 이하여야 합니다." }
        check_domain(value.matches(FORMAT_RULE)) { "회원 ID는 알파벳 소문자와 숫자로만 이루어져야 합니다." }
    }

    companion object {
        const val MIN_LENGTH: Int = 6
        const val MAX_LENGTH: Int = 20
        val FORMAT_RULE = Regex("^[a-z0-9]+$")
    }
}

@JvmInline
value class MemberName(val value: String) {
    init {
        check_domain(value.length in MIN_LENGTH..MAX_LENGTH) { "회원 이름은 ${MIN_LENGTH}자 이상 ${MAX_LENGTH}자 이하여야 합니다." }
    }

    companion object {
        const val MIN_LENGTH = 1
        const val MAX_LENGTH = 50
    }
}

data class MemberPhone(val value: String) {
    init {
        check_domain(value.matches(FORMAT_RULE)) { "회원 전화번호 양식은 +국가코드-번호-번호-번호 꼴이어야 합니다." }
    }

    companion object {
        val FORMAT_RULE = Regex("^\\+\\d{1,3}-\\d{1,4}-\\d{3,4}-\\d{4}$")
    }
}

data class MemberPlainPassword(val value: String) {
    init {
        check_domain(value.length in MIN_LENGTH..MAX_LENGTH) { "회원 비밀번호는 ${MIN_LENGTH}자 이상 ${MAX_LENGTH}자 이하여야 합니다." }
        check_domain(value.matches(FORMAT_RULE)) { "회원 비밀번호는 알파벳 대소문자와 숫자, 그리고 !@#\\\$%^&*만 사용할 수 있습니다." }
    }

    override fun toString(): String = "MemberPlainPassword[value=MASKED]"

    companion object {
        const val MIN_LENGTH = 6
        const val MAX_LENGTH = 30
        val FORMAT_RULE = Regex("^[a-zA-Z0-9!@#\$%^&*]+$")
    }
}

data class MemberHashedPassword (val value: String) {
    override fun toString(): String = "MemberHashedPassword[value=MASKED]"
}

interface MemberPasswordHashService {
    fun encode(plainPassword: MemberPlainPassword): MemberHashedPassword
    fun isEqual(plainPassword: MemberPlainPassword, hashedPassword: MemberHashedPassword): Boolean
}

// DB 매핑으로 불가피하게 Enum 사용 불가
data class MemberStatus(val value: Int) {
    init {
        require(value in ALLOWED_VALUES) { "회원 상태의 값이 잘못되었습니다. (현재 값 = $value)" }
    }

    companion object {
        private val ALLOWED_VALUES = setOf(0, 1)
        val MEMBER_WITHDREW = MemberStatus(0)
        val MEMBER_REGISTERED = MemberStatus(1)
    }
}

data class Member(
    val id: MemberId,
    val name: MemberName,
    val phone: MemberPhone,
    val password: MemberHashedPassword,
    val status: MemberStatus
)