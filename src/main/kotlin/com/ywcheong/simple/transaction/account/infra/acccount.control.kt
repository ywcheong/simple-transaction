package com.ywcheong.simple.transaction.account.infra

import com.ywcheong.simple.transaction.account.domain.*
import com.ywcheong.simple.transaction.common.service.PrincipalService
import com.ywcheong.simple.transaction.common.service.TransactionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

data class DepositRequest(val amount: Long)
data class WithdrawRequest(val amount: Long)
data class DepositResponse(val accountId: String, val newBalance: Long)
data class WithdrawResponse(val accountId: String, val newBalance: Long)

data class LookupEveryAccountsResponse(val accountIds: List<String>)
data class LookupOneAccountResponse(val balance: Long)
data class OpenAccountResponse(val accountId: String)

@RestController
@RequestMapping("/accounts")
class AccountController(
    private val principalService: PrincipalService,
    private val transactionService: TransactionService,
    private val accountRepository: AccountRepository,
    private val eventRepository: AccountEventRepository
) {
    @GetMapping("/")
    fun lookupEveryAccounts(): ResponseEntity<LookupEveryAccountsResponse> {
        // 요청 준비

        // 의존성 조율
        val memberId = principalService.getMemberId()
        val accounts = accountRepository.findAccountByOwner(memberId)
        val accountIds = accounts.map { it.id.value }

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(
            LookupEveryAccountsResponse(accountIds)
        )
    }

    @GetMapping("/{id}")
    fun lookupOneAccount(@PathVariable id: String): ResponseEntity<LookupOneAccountResponse> {
        // 요청 준비
        val accountId = AccountId(id)

        // 의존성 조율
        val account = getAccountWithCheck(accountId)

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(
            LookupOneAccountResponse(
                balance = account.balance.value
            )
        )
    }

    @PostMapping("/")
    fun openNewAccount(): ResponseEntity<OpenAccountResponse> {
        // 요청 준비
        val accountId = AccountId.createUnique()
        val zeroBalance = AccountBalance(0)

        // 의존성 조율
        val memberId = principalService.getMemberId()
        val account = Account(
            id = accountId, owner = memberId, balance = zeroBalance, version = 0
        )
        accountRepository.insert(account)

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(OpenAccountResponse(account.id.value))
    }

    @DeleteMapping("/{id}")
    fun closeExistingAccount(@PathVariable id: String): ResponseEntity<Nothing> {
        // 요청 준비
        val accountId = AccountId(id)

        // 의존성 조율
        val tx = transactionService.transaction()
        tx.required {
            val account = getAccountWithCheck(accountId)
            if (account.balance.value > 0) throw AccountBalanceNotZeroException()
            accountRepository.delete(account)
        }

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(null)
    }

    @PostMapping("/{id}/deposit")
    fun executeDeposit(
        @PathVariable id: String, @RequestBody request: DepositRequest
    ): ResponseEntity<DepositResponse> {
        // 요청 준비
        val accountId = AccountId(id)
        val depositChange = AccountBalanceChange(request.amount)

        val event = AccountDepositedEvent(
            id = AccountEventId.createUnique(), issuedAt = Date(), account = accountId, amount = depositChange
        )

        // 의존성 조율
        // TODO 멱등성
        var updatedBalance: AccountBalance? = null
        val tx = transactionService.transaction()

        tx.required {
            val account = getAccountWithCheck(accountId)
            val updatedAccount = account.deposit(depositChange)
            accountRepository.update(updatedAccount)
            eventRepository.insert(event)
            updatedBalance = updatedAccount.balance
        }

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(
            DepositResponse(
                accountId = id, newBalance = requireNotNull(updatedBalance).value
            )
        )
    }

    @PostMapping("/{id}/withdraw")
    fun executeWithdraw(
        @PathVariable id: String, @RequestBody request: WithdrawRequest
    ): ResponseEntity<WithdrawResponse> {
        // 요청 준비
        val accountId = AccountId(id)
        val withdrawChange = AccountBalanceChange(request.amount)

        val event = AccountWithdrewEvent(
            id = AccountEventId.createUnique(), issuedAt = Date(), account = accountId, amount = withdrawChange
        )

        // 의존성 조율
        // TODO 멱등성
        var updatedBalance: AccountBalance? = null
        val tx = transactionService.transaction()

        tx.required {
            val account = getAccountWithCheck(accountId)
            val updatedAccount = account.withdraw(withdrawChange)
            accountRepository.update(updatedAccount)
            eventRepository.insert(event)
            updatedBalance = updatedAccount.balance
        }

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(
            WithdrawResponse(
                accountId = id, newBalance = requireNotNull(updatedBalance).value
            )
        )
    }

    private fun getAccountWithCheck(accountId: AccountId): Account {
        val account = accountRepository.findAccountById(accountId) ?: throw AccountNotFoundException()
        val currentUser = principalService.getMemberId()
        if (account.owner != currentUser) throw AccountNotOwnedException()
        return account
    }
}

data class TransferRequest(val from: String, val to: String, val amount: Long)
data class TransferResponse(val eventId: String, val delayed: Boolean)

data class CheckTransferRequest(val isAccepted: Boolean, val reasonRejected: String? = null)

@RestController
@RequestMapping("/transfers")
class TransferController(
    private val principalService: PrincipalService,
    private val transactionService: TransactionService,
    private val accountRepository: AccountRepository,
    private val eventRepository: AccountEventRepository,
) {
    @PostMapping("/")
    fun executeTransfer(@RequestBody request: TransferRequest): ResponseEntity<TransferResponse> = with(request) {
        // 요청 준비
        val fromAccountId = AccountId(from)
        val toAccountId = AccountId(to)
        val balanceChange = AccountBalanceChange(amount)

        // 의존성 조율
        // TODO 멱등성

        val tx = transactionService.transaction()
        var response: TransferResponse? = null
        tx.required {
            // 송금자와 수금자를 로드합니다. 이때 송금자는 지금 로그인한 사람임을 검증합니다.
            val fromAccount = accountRepository.findAccountById(fromAccountId) ?: throw AccountNotFoundException()
            val currentUser = principalService.getMemberId()
            if (fromAccount.owner != currentUser) throw AccountNotOwnedException()
            val toAccount = accountRepository.findAccountById(toAccountId) ?: throw AccountNotFoundException()

            val transfer = Transfer(
                fromAccount = fromAccount, toAccount = toAccount, amount = balanceChange
            )

            // 고액 송금인지 판정한 뒤, 이체를 실행합니다.
            // 트랜잭션 및 낙관 락으로 일관성을 보장합니다.
            if (transfer.isLargeTransfer()) {
                val eventId = executeDelayedTransfer(transfer)
                response = TransferResponse(
                    eventId = eventId.value, delayed = true
                )
            } else {
                val eventId = executeImmediateTransfer(transfer)
                response = TransferResponse(
                    eventId = eventId.value, delayed = false
                )
            }
        }

        // 응답 반환
        if (response == null) {
            throw UnexpectedAccountTransferException()
        }

        if (response!!.delayed) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response)
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(response)
        }
    }

    fun executeImmediateTransfer(transfer: Transfer): AccountEventId = with(transfer) {
        val newFromAccount = fromAccount.withdraw(amount)
        val newToAccount = toAccount.deposit(amount)

        val event = AccountTransferAcceptedEvent(
            id = AccountEventId.createUnique(),
            previousId = null,
            issuedAt = Date(),
            accountFrom = fromAccount.id,
            accountTo = toAccount.id,
            amount = amount
        )

        accountRepository.update(newFromAccount)
        accountRepository.update(newToAccount)
        eventRepository.insert(event)

        event.id
    }

    fun executeDelayedTransfer(transfer: Transfer): AccountEventId = with(transfer) {
        val event = AccountTransferAttemptEvent(
            id = AccountEventId.createUnique(),
            issuedAt = Date(),
            accountFrom = fromAccount.id,
            accountTo = toAccount.id,
            amount = amount,
        )

        eventRepository.insert(event)
        event.id
    }

    @GetMapping("/{eventId}")
    fun checkTransfer(@PathVariable transferEventId: String, @RequestBody request: CheckTransferRequest) {
        // 요청 준비

        // 의존성 조율

        // 응답 반환
    }
}