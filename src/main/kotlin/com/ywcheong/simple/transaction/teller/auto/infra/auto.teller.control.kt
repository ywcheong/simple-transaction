package com.ywcheong.simple.transaction.teller.auto.infra

import com.ywcheong.simple.transaction.common.service.TransactionService
import com.ywcheong.simple.transaction.teller.auto.domain.AutoTeller
import com.ywcheong.simple.transaction.teller.auto.domain.AutoTellerRepository
import com.ywcheong.simple.transaction.teller.auto.domain.cash.Cash
import com.ywcheong.simple.transaction.teller.base.domain.*
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

data class CreateAutoTellerRequest(
    val id: String
)

data class DeleteAutoTellerRequest(
    val id: String
)

@RestController
@RequestMapping("/tellers/auto")
@PreAuthorize("hasRole('TELLER_ADMIN')")
class AutoTellerController(
    private val transactionService: TransactionService,
    private val tellerRepository: TellerRepository,
    private val autoTellerRepository: AutoTellerRepository
) : AutoTellerControllerSpec {

    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    override fun createAutoTeller(@RequestBody request: CreateAutoTellerRequest) {
        // 요청 준비
        val tellerId = TellerId(request.id)
        val teller = Teller(
            id = tellerId,
            type = TellerType.TELLER_AUTO,
            active = true,
            publicKey = null,
            permissions = setOf(),
            version = 0
        )
        val autoTeller = AutoTeller(
            teller = teller, vault = Cash.ZERO, version = 0
        )

        // 의존성 조율
        val tx = transactionService.transaction()
        tx.required {
            try {
                tellerRepository.insert(teller)
                autoTellerRepository.insert(autoTeller)
            } catch (_: DuplicateKeyException) {
                throw TellerAlreadyExistException()
            }
        }

        // 결과 반환
        return
    }

    @DeleteMapping("/")
    @ResponseStatus(HttpStatus.OK)
    override fun deleteAutoTeller(@RequestBody request: DeleteAutoTellerRequest) {
        // 요청 준비
        val tellerId = TellerId(request.id)

        // 의존성 조율
        val tx = transactionService.transaction()
        tx.required {
            val autoTeller = autoTellerRepository.select(tellerId)
            if (autoTeller == null) throw TellerNotFoundException()
            if (!autoTeller.vault.isZero()) throw AutoTellerVaultNonzeroException()
            tellerRepository.delete(autoTeller.teller)
        }

        // 결과 반환
        return
    }
}