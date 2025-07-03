package com.ywcheong.simple.transaction.member

import com.ywcheong.simple.transaction.exception.UserFaultException
import com.ywcheong.simple.transaction.member.domain.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class MemberIdTest {
    @Test
    fun `최소 길이의 회원 id는 생성된다`() {
        val id = "abcdef"

        val memberId = MemberId(id)

        assertEquals("abcdef", memberId.value)
    }

    @Test
    fun `최소 길이보다 짧은 id는 생성되지 않는다`() {
        val id = "abcde"

        assertThrows<InvalidMemberIdException> {
            MemberId(id)
        }
    }

    @Test
    fun `최대 길이의 회원 id는 생성된다`() {
        val id = "12345678901234567890"

        val memberId = MemberId(id)

        assertEquals("12345678901234567890", memberId.value)
    }

    @Test
    fun `최대 길이보다 긴 회원 id는 생성되지 않는다`() {
        val id = "123456789012345678901"

        assertThrows<InvalidMemberIdException> {
            MemberId(id)
        }
    }

    @Test
    fun `알파벳 소문자와 숫자를 혼합한 회원 id는 생성된다`() {
        val id = "aqz019"

        val memberId = MemberId(id)

        assertEquals("aqz019", memberId.value)
    }

    @Test
    fun `알파벳 대문자가 포함된 회원 id는 생성되지 않는다`() {
        val id = "Aqz019"

        assertThrows<InvalidMemberIdException> {
            MemberId(id)
        }
    }

    @Test
    fun `이상한 문자가 포함된 회원 id는 생성되지 않는다 - 한글`() {
        val id = "가나다라마바"

        assertThrows<InvalidMemberIdException> {
            MemberId(id)
        }
    }

    @Test
    fun `이상한 문자가 포함된 회원 id는 생성되지 않는다 - 이모지`() {
        val id = "\uD83D\uDE03\uD83D\uDE03\uD83D\uDE03\uD83D\uDE03\uD83D\uDE03\uD83D\uDE03"

        assertThrows<UserFaultException> {
            MemberId(id)
        }
    }
}

class MemberNameTest {
    @Test
    fun `최소 길이의 회원 이름은 생성된다`() {
        val name = "가"

        val memberName = MemberName(name)

        assertEquals("가", memberName.value)
    }

    @Test
    fun `최소 길이보다 짧은 회원 이름은 생성되지 않는다`() {
        val name = ""

        assertThrows<InvalidMemberNameException> {
            MemberName(name)
        }
    }

    @Test
    fun `최대 길이의 회원 이름은 생성된다`() {
        val name = "가".repeat(50)

        val memberName = MemberName(name)

        assertEquals("가".repeat(50), memberName.value)
    }

    @Test
    fun `최대 길이보다 긴 회원 이름은 생성되지 않는다`() {
        val name = "가".repeat(51)

        assertThrows<InvalidMemberNameException> {
            MemberName(name)
        }
    }
}

class MemberPhoneTest {
    @Test
    fun `정상적인 국제 전화번호는 생성된다`() {
        val phone = "+82-10-1234-5678"

        val memberPhone = MemberPhone(phone)

        assertEquals("+82-10-1234-5678", memberPhone.value)
    }

    @Test
    fun `형식에 맞지 않는 전화번호는 생성되지 않는다 - 하이픈 없음`() {
        val phone = "+821012345678"

        assertThrows<InvalidMemberPhoneException> {
            MemberPhone(phone)
        }
    }

    @Test
    fun `형식에 맞지 않는 전화번호는 생성되지 않는다 - 국가코드 없음`() {
        val phone = "010-1234-5678"

        assertThrows<InvalidMemberPhoneException> {
            MemberPhone(phone)
        }
    }

    @Test
    fun `형식에 맞지 않는 전화번호는 생성되지 않는다 - 숫자 아닌 문자 포함`() {
        val phone = "+82-10-1234-ABCD"

        assertThrows<InvalidMemberPhoneException> {
            MemberPhone(phone)
        }
    }
}

class MemberPlainPasswordTest {
    @Test
    fun `최소 길이의 비밀번호는 생성된다`() {
        val pw = "aB1!aB"

        val memberPw = MemberPlainPassword(pw)

        assertEquals("aB1!aB", memberPw.value)
    }

    @Test
    fun `최소 길이보다 짧은 비밀번호는 생성되지 않는다`() {
        val pw = "aB1!a"

        assertThrows<InvalidMemberPasswordException> {
            MemberPlainPassword(pw)
        }
    }

    @Test
    fun `최대 길이의 비밀번호는 생성된다`() {
        val pw = "aB1!@#".repeat(5) // 6*5=30

        val memberPw = MemberPlainPassword(pw)

        assertEquals("aB1!@#".repeat(5), memberPw.value)
    }

    @Test
    fun `최대 길이보다 긴 비밀번호는 생성되지 않는다`() {
        val pw = "aB1!@#".repeat(5) + "a" // 31자

        assertThrows<InvalidMemberPasswordException> {
            MemberPlainPassword(pw)
        }
    }

    @Test
    fun `허용된 특수문자만 포함된 비밀번호는 생성된다`() {
        val pw = "abcABC123!@#\$%^&*"

        val memberPw = MemberPlainPassword(pw)

        assertEquals("abcABC123!@#\$%^&*", memberPw.value)
    }

    @Test
    fun `허용되지 않은 특수문자가 포함된 비밀번호는 생성되지 않는다`() {
        val pw = "abcABC123()_"

        assertThrows<InvalidMemberPasswordException> {
            MemberPlainPassword(pw)
        }
    }
}

class MemberStatusTest {
    @Test
    fun `회원 상태가 등록 상태일 때 생성된다`() {
        val status = MemberStatus(MemberStatus.MEMBER_REGISTERED.value)

        assertEquals(MemberStatus.MEMBER_REGISTERED.value, status.value)
    }

    @Test
    fun `회원 상태가 탈퇴 상태일 때 생성된다`() {
        val status = MemberStatus(MemberStatus.MEMBER_WITHDREW.value)

        assertEquals(MemberStatus.MEMBER_WITHDREW.value, status.value)
    }

    @Test
    fun `회원 상태가 허용되지 않은 값이면 생성되지 않는다`() {
        val invalid = 2

        assertThrows<IllegalArgumentException> {
            MemberStatus(invalid)
        }
    }
}
