package com.ywcheong.simple.transaction.account.infra

import com.ywcheong.simple.transaction.account.domain.*
import com.ywcheong.simple.transaction.common.service.PrincipalService
import com.ywcheong.simple.transaction.common.service.TransactionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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
    private val accountRepository: AccountRepository
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

        // 의존성 조율
        // TODO 멱등성
        var updatedBalance: AccountBalance? = null
        val tx = transactionService.transaction()

        tx.required {
            val account = getAccountWithCheck(accountId_)
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
        @PathVariable id: String,
        @RequestBody request: WithdrawRequest
    ): ResponseEntity<WithdrawResponse> {
        // 요청 준비
        val accountId = AccountId(id)
        val depositChange = AccountBalanceChange(request.amount)

        // 의존성 조율
        // TODO 멱등성
        var updatedBalance: AccountBalance? = null
        val tx = transactionService.transaction()

        tx.required {
            val account = getAccountWithCheck(accountId)
            val updatedAccount = account.withdraw(depositChange)
            accountRepository.update(updatedAccount)
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
data class ProcessLargeTransferRequest(val isAccepted: Boolean, val reasonRejected: String? = null)

@RestController
@RequestMapping("/transfers")
class TransferController(
    private val principalService: PrincipalService,
    private val transactionService: TransactionService,
    private val accountRepository: AccountRepository
) {
    @PostMapping("/")
    fun executeTransfer(@RequestBody request: TransferRequest): ResponseEntity<Nothing> = with(request) {
        // 요청 준비
        val fromAccountId = AccountId(from)
        val toAccountId = AccountId(to)
        val balanceChange = AccountBalanceChange(amount)

        // 의존성 조율
        val tx = transactionService.transaction()
        tx.required {
            // 송금자와 수금자를 로드합니다. 이때 송금자는 지금 로그인한 사람임을 검증합니다.
            val fromAccount = accountRepository.findAccountById(fromAccountId) ?: throw AccountNotFoundException()
            val currentUser = principalService.getMemberId()
            if (fromAccount.owner != currentUser) throw AccountNotOwnedException()
            val toAccount = accountRepository.findAccountById(toAccountId) ?: throw AccountNotFoundException()

            // 이체를 실행합니다. 트랜잭션 및 낙관 락으로 일관성을 보장합니다.
            val updatedFromAccount = fromAccount.withdraw(balanceChange)
            val updatedToAccount = toAccount.deposit(balanceChange)

            // 영속성에 반영합니다.
            accountRepository.update(updatedFromAccount)
            accountRepository.update(updatedToAccount)
        }

        // 응답 반환
        ResponseEntity.status(HttpStatus.OK).body(null)
    }

    @PostMapping("/{transferEventId}")
    fun processLargeTransfer(@PathVariable transferEventId: String, @RequestBody request: ProcessLargeTransferRequest) {
        // 요청 준비

        // 의존성 조율

        // 응답 반환
    }
}