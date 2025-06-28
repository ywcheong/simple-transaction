package com.ywcheong.simple.transaction.member

data class MemberId(val value: String)
data class MemberName(val value: String)
data class MemberPhone(val value: String)
data class MemberPlainPassword(val value: String)
data class MemberHashedPassword(val value: String)

enum class MemberStatus (val value: Int) {
    MEMBER_WITHDREW(0), MEMBER_REGISTERED(1);

    companion object {
        fun fromValue(value: Int): MemberStatus? =
            entries.firstOrNull { it.value == value }
    }
}

data class Member(
    val id: MemberId,
    val name: MemberName,
    val phone: MemberPhone,
    val password: MemberHashedPassword,
    val status: MemberStatus
)