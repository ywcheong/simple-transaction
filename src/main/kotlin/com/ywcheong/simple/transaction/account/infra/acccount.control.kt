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
data class DepositResponse(val eventId: String, val newBalance: Long)
data class WithdrawResponse(val eventId: String, val newBalance: Long)

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
        val zeroPendingBalance = AccountPendingBalance(0)

        // 의존성 조율
        val memberId = principalService.getMemberId()
        val account = Account(
            id = accountId, owner = memberId, balance = zeroBalance, version = 0, pendingBalance = zeroPendingBalance
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
        val eventId = AccountEventId.createUnique()
        val depositChange = AccountBalanceChange(request.amount)

        // 의존성 조율
        // TODO 멱등성
        var updatedBalance: AccountBalance? = null
        val tx = transactionService.transaction()

        tx.required {
            val account = getAccountWithCheck(accountId)
            val updatedAccount = account.deposit(depositChange)
            accountRepository.update(updatedAccount)

            val event = AccountDepositedEvent(
                id = eventId, issuedAt = Date(), issuedBy = account.owner, account = accountId, amount = depositChange
            )

            eventRepository.insert(event)
            updatedBalance = updatedAccount.balance
        }

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(
            DepositResponse(
                eventId = eventId.value, newBalance = requireNotNull(updatedBalance).value
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
        val eventId = AccountEventId.createUnique()

        // 의존성 조율
        // TODO 멱등성
        var updatedBalance: AccountBalance? = null
        val tx = transactionService.transaction()

        tx.required {
            val account = getAccountWithCheck(accountId)
            val updatedAccount = account.withdraw(withdrawChange)
            accountRepository.update(updatedAccount)

            val event = AccountWithdrewEvent(
                id = eventId, issuedAt = Date(), issuedBy = account.owner, account = accountId, amount = withdrawChange
            )

            eventRepository.insert(event)
            updatedBalance = updatedAccount.balance
        }

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(
            WithdrawResponse(
                eventId = eventId.value, newBalance = requireNotNull(updatedBalance).value
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
data class TransferResponse(val eventId: String, val pending: Boolean)

data class CheckTransferResponse(val isPending: Boolean, val isAccepted: Boolean, val reasonRejected: String? = null)

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
        var transferEventId: AccountEventId? = null
        var transferResult: TransferResult? = null

        tx.required {
            // 송금자와 수금자를 로드합니다. 이때 송금자는 지금 로그인한 사람임을 검증합니다.
            val fromAccount = accountRepository.findAccountById(fromAccountId) ?: throw AccountNotFoundException()
            val currentUser = principalService.getMemberId()
            if (fromAccount.owner != currentUser) throw AccountNotOwnedException()
            val toAccount = accountRepository.findAccountById(toAccountId) ?: throw AccountNotFoundException()

            // 이체를 생성하고, 실행한 뒤 저장합니다.
            val transfer = Transfer(
                fromAccount = fromAccount, toAccount = toAccount, amount = balanceChange
            )
            val result = transfer.execute()
            transferEventId = saveTransferResult(result)
            transferResult = result
        }

        return when (requireNotNull(transferResult)) {
            is TransferResult.Pending -> ResponseEntity.status(HttpStatus.ACCEPTED).body(
                TransferResponse(
                    eventId = requireNotNull(transferEventId).value, pending = true
                )
            )

            is TransferResult.Complete -> ResponseEntity.status(HttpStatus.OK).body(
                TransferResponse(
                    eventId = requireNotNull(transferEventId).value, pending = false
                )
            )
        }
    }

    private fun saveTransferResult(result: TransferResult): AccountEventId = when (result) {
        is TransferResult.Pending -> savePendingTransfer(result)
        is TransferResult.Complete -> saveImmediateTransfer(result)
    }

    private fun saveImmediateTransfer(result: TransferResult.Complete): AccountEventId = with(result) {
        val event = AccountTransferAcceptedEvent(
            id = AccountEventId.createUnique(),
            issuedAt = Date(),
            issuedBy = fromAccount.owner,
            accountFrom = fromAccount.id,
            accountTo = toAccount.id,
            amount = amount,
        )

        accountRepository.update(fromAccount)
        accountRepository.update(toAccount)
        eventRepository.insert(event)

        event.id
    }

    private fun savePendingTransfer(result: TransferResult.Pending): AccountEventId = with(result) {
        val event = AccountTransferAttemptEvent(
            id = AccountEventId.createUnique(),
            issuedAt = Date(),
            issuedBy = fromAccount.owner,
            accountFrom = fromAccount.id,
            accountTo = toAccount.id,
            amount = amount,
            subsequentId = null
        )

        accountRepository.update(fromAccount)
        accountRepository.update(toAccount)
        eventRepository.insert(event)

        event.id
    }

    @GetMapping("/{id}")
    fun checkTransfer(@PathVariable id: String): ResponseEntity<CheckTransferResponse> {
        // 요청 준비
        val eventId = AccountEventId(id)

        // 의존성 조율
        val response = findEventById(eventId)
        return ResponseEntity.status(HttpStatus.OK).body(response)
    }

    private fun findEventById(eventId: AccountEventId): CheckTransferResponse {
        val event: AccountEvent = eventRepository.findById(eventId) ?: throw AccountTransferEventNotFoundException()
        val currentUser = principalService.getMemberId()
        if (event.issuedBy != currentUser) throw AccountNotOwnedException()

        when (event.type) {
            AccountEventType.TRANSFER_ACCEPT -> {
                return CheckTransferResponse(
                    isPending = false, isAccepted = true, reasonRejected = null
                )
            }

            AccountEventType.TRANSFER_ATTEMPT -> {
                if (event.subsequentId != null) return findEventById(event.subsequentId!!)
                return CheckTransferResponse(
                    isPending = true, isAccepted = false, reasonRejected = null
                )
            }

            AccountEventType.TRANSFER_REJECT -> {
                return CheckTransferResponse(
                    isPending = false, isAccepted = false, reasonRejected = event.reason
                )
            }

            else -> {
                throw AccountEventNotTransferException()
            }
        }
    }
}