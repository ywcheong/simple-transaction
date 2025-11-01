package com.ywcheong.simple.transaction.teller.base.domain

import com.ywcheong.simple.transaction.certification.domain.CertificationMessage
import com.ywcheong.simple.transaction.certification.domain.CertificationPublicKey
import com.ywcheong.simple.transaction.certification.domain.CertificationSignedMessage

@JvmInline
value class TellerId(
    val value: String
)

@JvmInline
value class TellerType(
    val value: Int
) {
    init {
        if (value !in ALLOWED_VALUES) throw UnexpectedTellerTypeException(value)
    }

    companion object {
        private val ALLOWED_VALUES = setOf(0, 1)
        val TELLER_AUTO = TellerType(0)
        val TELLER_EMPLOYEE = TellerType(1)
    }
}

@JvmInline
value class TellerPermission(
    val value: String
)

data class Teller(
    val id: TellerId,
    val type: TellerType,
    val active: Boolean,
    val publicKey: CertificationPublicKey?,
    val permissions: Set<TellerPermission>,
    val version: Long
) {
    // 이 텔러 도메인 객체는 서버 측 객체로 서명을 위한 개인키는 오직 텔러 클라이언트만 저장하고 있다.
    // 따라서 서버에서 텔러 도메인 객체로는 서명을 시도할 수 없으며, 텔러 클라이언트가 생성한 서명을 검증하는 것만 가능하다.
    fun sign(message: CertificationMessage): Nothing = throw UnexpectedTellerSignAttemptException()
    fun verify(signedMessage: CertificationSignedMessage): CertificationMessage? =
        publicKey?.let { signedMessage.verify(it) } ?: throw TellerCertificateEmptyException()

    fun grant(newPermission: TellerPermission): Teller = this.copy(permissions = permissions + newPermission)
    fun revoke(oldPermission: TellerPermission): Teller = this.copy(permissions = permissions - oldPermission)
}