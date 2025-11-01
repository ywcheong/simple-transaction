package com.ywcheong.simple.transaction.teller.base.infra

import com.ywcheong.simple.transaction.certification.domain.CertificationPrivateKey
import com.ywcheong.simple.transaction.certification.domain.CertificationPublicKey
import com.ywcheong.simple.transaction.certification.domain.CertificationService
import com.ywcheong.simple.transaction.certification.domain.CertificationSignedMessage
import com.ywcheong.simple.transaction.common.service.TransactionService
import com.ywcheong.simple.transaction.teller.base.domain.*
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/tellers")
class TellerController(
    private val certificationService: CertificationService,
    private val transactionService: TransactionService,
    private val tellerRepository: TellerRepository
) : TellerControllerSpec {
    private fun selectTellerWithExistCheck(tellerId: TellerId): Teller =
        tellerRepository.select(tellerId) ?: throw TellerNotFoundException()

    @GetMapping("/{tellerId}/permissions")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('TELLER_ADMIN')")
    override fun lookupEmployeeTellerPermissions(@PathVariable tellerId: String): LookupTellerPermissionResponse {
        // 요청 준비
        val tellerId = TellerId(tellerId)

        // 의존성 조율
        val teller = selectTellerWithExistCheck(tellerId)

        // 응답 반환
        return LookupTellerPermissionResponse(
            permissions = teller.permissions.map { it.value })
    }

    @PostMapping("/{tellerId}/permissions/{grantingPermission}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('TELLER_ADMIN')")
    override fun grantEmployeeTellerPermission(
        @PathVariable tellerId: String, @PathVariable grantingPermission: String
    ): GrantTellerPermissionResponse {
        // 요청 준비
        val tellerId = TellerId(tellerId)
        val grantingPermission = TellerPermission(grantingPermission)

        // 의존성 조율
        var before: Boolean? = null

        val tx = transactionService.transaction()
        tx.required {
            val teller = selectTellerWithExistCheck(tellerId)
            before = grantingPermission in teller.permissions
            val newTeller = teller.copy(permissions = teller.permissions + grantingPermission)
            tellerRepository.update(newTeller)
        }

        // 응답 반환
        return GrantTellerPermissionResponse(
            permission = grantingPermission.value, hasPermissionBefore = before!!
        )
    }

    @DeleteMapping("/{tellerId}/permissions/{revokingPermission}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('TELLER_ADMIN')")
    override fun revokeEmployeeTellerPermission(
        @PathVariable tellerId: String,
        @PathVariable revokingPermission: String
    ): RevokeTellerPermissionResponse {
        // 요청 준비
        val tellerId = TellerId(tellerId)
        val revokingPermission = TellerPermission(revokingPermission)

        // 의존성 조율
        var before: Boolean? = null

        val tx = transactionService.transaction()
        tx.required {
            val teller = selectTellerWithExistCheck(tellerId)
            before = revokingPermission in teller.permissions
            val newTeller = teller.copy(permissions = teller.permissions - revokingPermission)
            tellerRepository.update(newTeller)
        }

        // 응답 반환
        return RevokeTellerPermissionResponse(
            permission = revokingPermission.value, hasPermissionBefore = before!!
        )
    }

    @PostMapping("/{tellerId}/key")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TELLER_ADMIN')")
    override fun createSigningKey(@PathVariable tellerId: String): CreateSigningKeyResponse {
        // 요청 준비
        val tellerId = TellerId(tellerId)

        // 의존성 조율
        val (privateKey: CertificationPrivateKey, publicKey: CertificationPublicKey) = certificationService.generateKeypair()

        val tx = transactionService.transaction()
        tx.required {
            val teller = selectTellerWithExistCheck(tellerId)
            if (teller.publicKey != null) throw TellerPublicKeyAlreadyExistException()
            val newTeller = teller.copy(publicKey = publicKey)
            tellerRepository.update(newTeller)
        }

        // 응답 반환
        return CreateSigningKeyResponse(tellerPrivateKey = privateKey.value)
    }

    @DeleteMapping("/{tellerId}/key")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('TELLER_ADMIN')")
    override fun invalidateSigningKey(@PathVariable tellerId: String) {
        // 요청 준비
        val tellerId = TellerId(tellerId)

        // 의존성 조율
        val tx = transactionService.transaction()
        tx.required {
            val teller = selectTellerWithExistCheck(tellerId)
            if (teller.publicKey == null) throw TellerPublicKeyNotFoundException()
            val newTeller = teller.copy(publicKey = null)
            tellerRepository.update(newTeller)
        }

        // 응답 반환
        return
    }

    @PostMapping("/{signedTellerId}/verification")
    @ResponseStatus(HttpStatus.OK)
    override fun verifySign(
        @PathVariable signedTellerId: String,
        @RequestBody request: VerifySignRequest
    ): VerifySignResponse {
        // 요청 준비
        val signedTellerId = TellerId(signedTellerId)
        val checkingPermission = TellerPermission(request.checkingPermission)
        val signedMessage = CertificationSignedMessage(request.signedMessage)

        // 의존성 조율
        val teller = selectTellerWithExistCheck(signedTellerId)
        if (checkingPermission !in teller.permissions) throw TellerInsufficientPermissionException()
        if (teller.publicKey == null) throw TellerPublicKeyNotFoundException()

        val message = signedMessage.verify(teller.publicKey)

        // 응답 반환
        return VerifySignResponse(
            message = message.value
        )
    }
}