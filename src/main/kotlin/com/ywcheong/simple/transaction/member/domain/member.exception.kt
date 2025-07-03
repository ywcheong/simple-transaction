package com.ywcheong.simple.transaction.member.domain

import com.ywcheong.simple.transaction.exception.UserFaultException

open class MemberException(msg: String) : UserFaultException(msg)
class InvalidMemberIdException(msg: String) : MemberException(msg)
class InvalidMemberNameException(msg: String) : MemberException(msg)
class InvalidMemberPhoneException(msg: String) : MemberException(msg)
class InvalidMemberPasswordException(msg: String) : MemberException(msg)

class DuplicateMemberIdException : MemberException("이미 존재하는 회원 ID입니다.")
class DeletedMemberException : MemberException("이미 삭제된 회원입니다.")
class MemberLoginException : MemberException("회원 ID 또는 비밀번호가 틀렸습니다.")