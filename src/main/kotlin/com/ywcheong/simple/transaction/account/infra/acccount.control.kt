package com.ywcheong.simple.transaction.account.infra

import com.ywcheong.simple.transaction.account.domain.*
import com.ywcheong.simple.transaction.common.service.PrincipalService
import com.ywcheong.simple.transaction.common.service.TransactionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

data class DepositRequest(val amount: Long)
data class WithdrawRequest(val amount: Long)
data class DepositResponse(val eventId: String, val newBalance: Long)
data class WithdrawResponse(val eventId: String, val newBalance: Long)

data class LookupEveryAccountsResponse(val accountIds: List<String>)
data class LookupOneAccountResponse(val balance: Long, val pendingBalance: Long)
data class OpenAccountResponse(val accountId: String)

@RestController
@RequestMapping("/accounts")
@Tag(name = "계좌 API", description = "계좌 관련 기능")
class AccountController(
    private val principalService: PrincipalService,
    private val transactionService: TransactionService,
    private val accountRepository: AccountRepository,
    private val eventRepository: AccountEventRepository
) {
    @Operation(
        summary = "계좌 목록 조회",
        description = "현재 인증된 사용자가 소유한 모든 계좌의 ID 목록을 반환합니다.",
        security = [SecurityRequirement(name = "bearer-key")],
        responses = [ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(schema = Schema(implementation = LookupEveryAccountsResponse::class))]
        ), ApiResponse(responseCode = "403", description = "인증 실패", content = [Content()])]
    )
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

    @Operation(
        summary = "계좌 상세 조회",
        description = "계좌 ID로 단일 계좌의 잔액과 보류 잔액을 조회합니다. 본인 소유 계좌만 조회 가능합니다.",
        security = [SecurityRequirement(name = "bearer-key")],
        parameters = [Parameter(name = "id", description = "계좌 ID (UUID4)", required = true, `in` = ParameterIn.PATH)],
        responses = [ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(schema = Schema(implementation = LookupOneAccountResponse::class))]
        ), ApiResponse(responseCode = "400", description = "잘못된 계좌 ID 또는 권한 없음", content = [Content()]), ApiResponse(
            responseCode = "403", description = "인증 실패", content = [Content()]
        )]
    )
    @GetMapping("/{id}")
    fun lookupOneAccount(@PathVariable id: String): ResponseEntity<LookupOneAccountResponse> {
        // 요청 준비
        val accountId = AccountId(id)

        // 의존성 조율
        val account = getAccountWithCheck(accountId)

        // 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(
            LookupOneAccountResponse(
                balance = account.balance.value, pendingBalance = account.pendingBalance.value
            )
        )
    }

    @Operation(
        summary = "계좌 개설",
        description = "새로운 계좌를 개설합니다. 반환값으로 개설된 계좌 ID를 제공합니다.",
        security = [SecurityRequirement(name = "bearer-key")],
        responses = [ApiResponse(
            responseCode = "200",
            description = "개설 성공",
            content = [Content(schema = Schema(implementation = OpenAccountResponse::class))]
        ), ApiResponse(responseCode = "403", description = "인증 실패", content = [Content()])]
    )
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

    @Operation(
        summary = "계좌 폐쇄",
        description = "계좌를 폐쇄(비활성화)합니다. 잔고가 0이어야만 폐쇄 가능합니다.",
        security = [SecurityRequirement(name = "bearer-key")],
        parameters = [Parameter(name = "id", description = "계좌 ID (UUID4)", required = true, `in` = ParameterIn.PATH)],
        responses = [ApiResponse(responseCode = "200", description = "폐쇄 성공", content = [Content()]), ApiResponse(
            responseCode = "400", description = "잔고가 남아 있거나 권한 없음", content = [Content()]
        ), ApiResponse(responseCode = "403", description = "인증 실패", content = [Content()])]
    )
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

    @Operation(
        summary = "계좌 입금",
        description = "지정한 계좌에 금액을 입금합니다.",
        security = [SecurityRequirement(name = "bearer-key")],
        parameters = [Parameter(name = "id", description = "계좌 ID (UUID4)", required = true, `in` = ParameterIn.PATH)],
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = DepositRequest::class),
                examples = [ExampleObject(value = "{\"amount\": 1000}")]
            )]
        ),
        responses = [ApiResponse(
            responseCode = "200",
            description = "입금 성공",
            content = [Content(schema = Schema(implementation = DepositResponse::class))]
        ), ApiResponse(
            responseCode = "400", description = "잘못된 요청(음수/0 금액, 권한 없음 등)", content = [Content()]
        ), ApiResponse(responseCode = "403", description = "인증 실패", content = [Content()])]
    )
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

    @Operation(
        summary = "계좌 출금",
        description = "지정한 계좌에서 금액을 출금합니다.",
        security = [SecurityRequirement(name = "bearer-key")],
        parameters = [Parameter(name = "id", description = "계좌 ID (UUID4)", required = true, `in` = ParameterIn.PATH)],
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = WithdrawRequest::class),
                examples = [ExampleObject(value = "{\"amount\": 500}")]
            )]
        ),
        responses = [ApiResponse(
            responseCode = "200",
            description = "출금 성공",
            content = [Content(schema = Schema(implementation = WithdrawResponse::class))]
        ), ApiResponse(
            responseCode = "400", description = "잘못된 요청(음수/0 금액, 잔고 부족, 권한 없음 등)", content = [Content()]
        ), ApiResponse(responseCode = "403", description = "인증 실패", content = [Content()])]
    )
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
@Tag(name = "송금 API", description = "송금 관련 기능")
class TransferController(
    private val principalService: PrincipalService,
    private val transactionService: TransactionService,
    private val accountRepository: AccountRepository,
    private val eventRepository: AccountEventRepository,
) {
    @Operation(
        summary = "계좌 송금",
        description = "계좌 간 송금을 실행합니다. 고액 송금(타인에게 100만원 이상)은 대기 상태가 됩니다.",
        security = [SecurityRequirement(name = "bearer-key")],
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = TransferRequest::class),
                examples = [ExampleObject(value = "{\"from\": \"계좌ID1\", \"to\": \"계좌ID2\", \"amount\": 1000000}")]
            )]
        ),
        responses = [ApiResponse(
            responseCode = "200", description = "즉시 송금 성공", content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = TransferResponse::class),
                examples = [ExampleObject(
                    name = "즉시송금예시",
                    summary = "즉시 송금 성공 예시",
                    value = "{\"eventId\": \"1142d4f9-279e-4828-9662-d8382923449b\", \"pending\": false}"
                )]
            )]
        ), ApiResponse(
            responseCode = "202", description = "고액 송금 대기", content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = TransferResponse::class),
                examples = [ExampleObject(
                    name = "고액송금예시",
                    summary = "고액 송금 대기 예시",
                    value = "{\"eventId\": \"1142d4f9-279e-4828-9662-d8382923449b\", \"pending\": true}"
                )]
            )]
        ), ApiResponse(
            responseCode = "400", description = "잘못된 요청(음수/0 금액, 잔고 부족, 권한 없음 등)", content = [Content()]
        ), ApiResponse(responseCode = "403", description = "인증 실패", content = [Content()])]
    )
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

    @Operation(
        summary = "송금 처리 상태 확인",
        description = "송금 이벤트 ID로 송금의 처리 상태(대기/완료/거부)를 조회합니다.",
        security = [SecurityRequirement(name = "bearer-key")],
        parameters = [Parameter(
            name = "id", description = "송금 이벤트 ID (UUID4)", required = true, `in` = ParameterIn.PATH
        )],
        responses = [ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(schema = Schema(implementation = CheckTransferResponse::class))]
        ), ApiResponse(responseCode = "400", description = "잘못된 이벤트 ID/권한 없음", content = [Content()]), ApiResponse(
            responseCode = "403", description = "인증 실패", content = [Content()]
        )]
    )
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