package com.ywcheong.simple.transaction.teller.base.domain

import com.ywcheong.simple.transaction.common.exception.UserFaultException

sealed class TellerException(msg: String) : UserFaultException(msg)
class TellerCertificateEmptyException : TellerException("텔러의 공개키가 서버에서 제거되어 텔러의 서명을 검증할 수 없습니다.")
class TellerNotFoundException : TellerException("텔러를 찾을 수 없습니다.")
class TellerAlreadyExistException : TellerException("사용 중인 텔러 아이디입니다.")
class TellerPublicKeyAlreadyExistException : TellerException("이미 서명용 개인키를 발급받은 텔러입니다.")
class TellerPublicKeyNotFoundException : TellerException("서명용 개인키를 발급받지 않은 텔러입니다.")
class TellerInsufficientPermissionException : TellerException("텔러에게 해당 권한이 없습니다.")

sealed class AutoTellerException(msg: String): TellerException(msg)
class AutoTellerVaultNonzeroException : AutoTellerException("자동 텔러의 금고가 비어 있지 않습니다.")

sealed class UnexpectedTellerException(msg: String) : RuntimeException(msg)
class UnexpectedTellerSignAttemptException : UnexpectedTellerException("텔러의 서명은 서버가 아닌 텔러 클라이언트만 서명할 수 있습니다.")
class UnexpectedTellerTypeException(value: Int) : UnexpectedTellerException("텔러의 유형값이 잘못되었습니다. (값=$value)")

sealed class UnexpectedTellerRepositoryException(msg: String) : UnexpectedTellerException(msg)
class UnexpectedTellerRepositoryUpdateException : UnexpectedTellerRepositoryException("텔러 DB 갱신에 실패했습니다.")
class UnexpectedTellerRepositoryInsertException : UnexpectedTellerRepositoryException("텔러 DB 삽입에 실패했습니다.")
class UnexpectedTellerRepositoryNullException(name: String) : UnexpectedTellerRepositoryException("텔러 DB 엔티티의 참조가 null 입니다. (필드=${name})")