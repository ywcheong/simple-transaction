package com.ywcheong.simple.transaction.account

import com.ywcheong.simple.transaction.member.infra.RegisterRequest
import com.ywcheong.simple.transaction.member.infra.RegisterResponse
import com.ywcheong.simple.transaction.member.infra.TokenRequest
import com.ywcheong.simple.transaction.member.infra.TokenResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountIntegrationTest @Autowired constructor(
    private val rest: TestRestTemplate, private val jdbc: JdbcTemplate
) {
    companion object {
        val userAId = "idofusera"
        val userAName = "유저에이"
        val userAPhone = "+1-23-4567-8901"
        val userAPassword = "password1@"

        val userBId = "idofuserb"
        val userBName = "유저비이"
        val userBPhone = "+82-10-1234-5678"
        val userBPassword = "1q2w3e4r!"
    }

    @BeforeEach
    fun setup() {
        jdbc.execute("DELETE FROM account WHERE TRUE")
        jdbc.execute("DELETE FROM member WHERE TRUE")
    }

    fun registerAndLogin(id: String, name: String, pw: String, phone: String): String {
        rest.postForEntity("/members", RegisterRequest(id, name, pw, phone), RegisterResponse::class.java)
        val tokenRes = rest.postForEntity("/members/tokens", TokenRequest(id, pw), TokenResponse::class.java)
        return tokenRes.body!!.token
    }

    fun openAccount(token: String): String {
        val headers = HttpHeaders().apply { setBearerAuth(token) }
        val res = rest.postForEntity("/accounts/", HttpEntity<Void>(headers), OpenAccountResponse::class.java)
        return res.body!!.accountId
    }

    @Test
    fun `사전 준비가 정상적으로 동작한다`() {
        val tokenA = registerAndLogin(userAId, userAName, userAPassword, userAPhone)
        val accountA1 = openAccount(tokenA)
        val tokenB = registerAndLogin(userBId, userBName, userBPassword, userBPhone)
        val accountB1 = openAccount(tokenB)
        val accountB2 = openAccount(tokenB)
        assertNotNull(tokenA)
        assertNotNull(tokenB)
        assertNotNull(accountA1)
        assertNotNull(accountB1)
        assertNotNull(accountB2)
    }

    @Test
    fun `계좌 서비스 통합 시나리오`() {
        val tokenA = registerAndLogin(userAId, userAName, userAPassword, userAPhone)
        val accountA1 = openAccount(tokenA)
        val tokenB = registerAndLogin(userBId, userBName, userBPassword, userBPhone)
        val accountB1 = openAccount(tokenB)
        val accountB2 = openAccount(tokenB)

        val accountsA = rest.exchange(
            "/accounts/",
            HttpMethod.GET,
            HttpEntity<Void>(HttpHeaders().apply { setBearerAuth(tokenA) }),
            LookupEveryAccountsResponse::class.java
        )
        val accountsB = rest.exchange(
            "/accounts/",
            HttpMethod.GET,
            HttpEntity<Void>(HttpHeaders().apply { setBearerAuth(tokenB) }),
            LookupEveryAccountsResponse::class.java
        )
        assertEquals(listOf(accountA1), accountsA.body!!.accountIds)
        assertEquals(setOf(accountB1, accountB2), accountsB.body!!.accountIds.toSet())

        rest.postForEntity(
            "/accounts/$accountA1/deposit",
            HttpEntity(DepositRequest(500), HttpHeaders().apply { setBearerAuth(tokenA) }),
            DepositResponse::class.java
        )
        val balanceA1_1 = rest.exchange(
            "/accounts/$accountA1",
            HttpMethod.GET,
            HttpEntity<Void>(HttpHeaders().apply { setBearerAuth(tokenA) }),
            LookupOneAccountResponse::class.java
        )
        assertEquals(500, balanceA1_1.body!!.balance)

        rest.postForEntity(
            "/accounts/$accountA1/withdraw",
            HttpEntity(WithdrawRequest(200), HttpHeaders().apply { setBearerAuth(tokenA) }),
            WithdrawResponse::class.java
        )
        val balanceA1_2 = rest.exchange(
            "/accounts/$accountA1",
            HttpMethod.GET,
            HttpEntity<Void>(HttpHeaders().apply { setBearerAuth(tokenA) }),
            LookupOneAccountResponse::class.java
        )
        assertEquals(300, balanceA1_2.body!!.balance)

        rest.postForEntity(
            "/accounts/$accountB1/deposit",
            HttpEntity(DepositRequest(1000), HttpHeaders().apply { setBearerAuth(tokenB) }),
            DepositResponse::class.java
        )
        rest.postForEntity(
            "/transfers/",
            HttpEntity(TransferRequest(accountB1, accountA1, 700), HttpHeaders().apply { setBearerAuth(tokenB) }),
            String::class.java
        )
        val balanceA1_3 = rest.exchange(
            "/accounts/$accountA1",
            HttpMethod.GET,
            HttpEntity<Void>(HttpHeaders().apply { setBearerAuth(tokenA) }),
            LookupOneAccountResponse::class.java
        )
        val balanceB1_1 = rest.exchange(
            "/accounts/$accountB1",
            HttpMethod.GET,
            HttpEntity<Void>(HttpHeaders().apply { setBearerAuth(tokenB) }),
            LookupOneAccountResponse::class.java
        )
        assertEquals(1000, balanceA1_3.body!!.balance)
        assertEquals(300, balanceB1_1.body!!.balance)

        rest.postForEntity(
            "/transfers/",
            HttpEntity(TransferRequest(accountB1, accountB2, 300), HttpHeaders().apply { setBearerAuth(tokenB) }),
            String::class.java
        )
        val balanceB1_2 = rest.exchange(
            "/accounts/$accountB1",
            HttpMethod.GET,
            HttpEntity<Void>(HttpHeaders().apply { setBearerAuth(tokenB) }),
            LookupOneAccountResponse::class.java
        )
        val balanceB2_1 = rest.exchange(
            "/accounts/$accountB2",
            HttpMethod.GET,
            HttpEntity<Void>(HttpHeaders().apply { setBearerAuth(tokenB) }),
            LookupOneAccountResponse::class.java
        )
        assertEquals(0, balanceB1_2.body!!.balance)
        assertEquals(300, balanceB2_1.body!!.balance)

        rest.exchange(
            "/accounts/$accountB1",
            HttpMethod.DELETE,
            HttpEntity<Void>(HttpHeaders().apply { setBearerAuth(tokenB) }),
            String::class.java
        )
        val accountsBAfter = rest.exchange(
            "/accounts/",
            HttpMethod.GET,
            HttpEntity<Void>(HttpHeaders().apply { setBearerAuth(tokenB) }),
            LookupEveryAccountsResponse::class.java
        )
        assertEquals(setOf(accountB2), accountsBAfter.body!!.accountIds.toSet())
        val b1Lookup = rest.exchange(
            "/accounts/$accountB1",
            HttpMethod.GET,
            HttpEntity<Void>(HttpHeaders().apply { setBearerAuth(tokenB) }),
            String::class.java
        )
        assertEquals(HttpStatus.BAD_REQUEST, b1Lookup.statusCode)
    }

    @Test
    fun `타인 계좌목록은 조회할 수 없다`() {
        val tokenA = registerAndLogin(userAId, userAName, userAPassword, userAPhone)
        val tokenB = registerAndLogin(userBId, userBName, userBPassword, userBPhone)
        openAccount(tokenB)
        openAccount(tokenB)
        val response = rest.exchange(
            "/accounts/",
            HttpMethod.GET,
            HttpEntity<Void>(HttpHeaders().apply { setBearerAuth(tokenA) }),
            LookupEveryAccountsResponse::class.java
        )
        assertEquals(0, response.body!!.accountIds.size)
    }

    @Test
    fun `타인 계좌에 접근, 입출금, 송금 불가`() {
        val tokenA = registerAndLogin(userAId, userAName, userAPassword, userAPhone)
        val tokenB = registerAndLogin(userBId, userBName, userBPassword, userBPhone)
        val accountB1 = openAccount(tokenB)
        val headersA = HttpHeaders().apply { setBearerAuth(tokenA) }
        assertEquals(
            HttpStatus.BAD_REQUEST, rest.exchange(
                "/accounts/$accountB1", HttpMethod.GET, HttpEntity<Void>(headersA), String::class.java
            ).statusCode
        )
        assertEquals(
            HttpStatus.BAD_REQUEST, rest.postForEntity(
                "/accounts/$accountB1/deposit", HttpEntity(DepositRequest(100), headersA), String::class.java
            ).statusCode
        )
        assertEquals(
            HttpStatus.BAD_REQUEST, rest.postForEntity(
                "/accounts/$accountB1/withdraw", HttpEntity(WithdrawRequest(100), headersA), String::class.java
            ).statusCode
        )
        assertEquals(
            HttpStatus.BAD_REQUEST, rest.postForEntity(
                "/transfers/", HttpEntity(TransferRequest(accountB1, accountB1, 100), headersA), String::class.java
            ).statusCode
        )
    }

    @Test
    fun `존재하지 않는 계좌 조회 불가`() {
        val tokenA = registerAndLogin(userAId, userAName, userAPassword, userAPhone)
        val fakeAccountId = "00000000-0000-4000-8000-000000000000"
        val response = rest.exchange(
            "/accounts/$fakeAccountId",
            HttpMethod.GET,
            HttpEntity<Void>(HttpHeaders().apply { setBearerAuth(tokenA) }),
            String::class.java
        )
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `잔액 남은 계좌는 폐쇄 불가`() {
        val tokenA = registerAndLogin(userAId, userAName, userAPassword, userAPhone)
        val accountA1 = openAccount(tokenA)
        rest.postForEntity(
            "/accounts/$accountA1/deposit",
            HttpEntity(DepositRequest(500), HttpHeaders().apply { setBearerAuth(tokenA) }),
            DepositResponse::class.java
        )
        val closeRes = rest.exchange(
            "/accounts/$accountA1",
            HttpMethod.DELETE,
            HttpEntity<Void>(HttpHeaders().apply { setBearerAuth(tokenA) }),
            String::class.java
        )
        val balanceRes = rest.exchange(
            "/accounts/$accountA1",
            HttpMethod.GET,
            HttpEntity<Void>(HttpHeaders().apply { setBearerAuth(tokenA) }),
            LookupOneAccountResponse::class.java
        )
        assertEquals(HttpStatus.BAD_REQUEST, closeRes.statusCode)
        assertEquals(500, balanceRes.body!!.balance)
    }

    @Test
    fun `잔액 초과 출금 불가`() {
        val tokenA = registerAndLogin(userAId, userAName, userAPassword, userAPhone)
        val accountA1 = openAccount(tokenA)
        rest.postForEntity(
            "/accounts/$accountA1/deposit",
            HttpEntity(DepositRequest(500), HttpHeaders().apply { setBearerAuth(tokenA) }),
            DepositResponse::class.java
        )
        val withdrawRes = rest.postForEntity(
            "/accounts/$accountA1/withdraw",
            HttpEntity(WithdrawRequest(501), HttpHeaders().apply { setBearerAuth(tokenA) }),
            String::class.java
        )
        val balanceRes = rest.exchange(
            "/accounts/$accountA1",
            HttpMethod.GET,
            HttpEntity<Void>(HttpHeaders().apply { setBearerAuth(tokenA) }),
            LookupOneAccountResponse::class.java
        )
        assertEquals(HttpStatus.BAD_REQUEST, withdrawRes.statusCode)
        assertEquals(500, balanceRes.body!!.balance)
    }

    @Test
    fun `음수 금액 입출금, 송금 불가`() {
        val tokenA = registerAndLogin(userAId, userAName, userAPassword, userAPhone)
        val accountA1 = openAccount(tokenA)
        rest.postForEntity(
            "/accounts/$accountA1/deposit",
            HttpEntity(DepositRequest(500), HttpHeaders().apply { setBearerAuth(tokenA) }),
            DepositResponse::class.java
        )
        assertEquals(
            HttpStatus.BAD_REQUEST, rest.postForEntity(
                "/accounts/$accountA1/deposit",
                HttpEntity(DepositRequest(-300), HttpHeaders().apply { setBearerAuth(tokenA) }),
                String::class.java
            ).statusCode
        )
        assertEquals(
            HttpStatus.BAD_REQUEST, rest.postForEntity(
                "/accounts/$accountA1/withdraw",
                HttpEntity(WithdrawRequest(-300), HttpHeaders().apply { setBearerAuth(tokenA) }),
                String::class.java
            ).statusCode
        )
        assertEquals(
            HttpStatus.BAD_REQUEST, rest.postForEntity(
                "/transfers/",
                HttpEntity(TransferRequest(accountA1, accountA1, -300), HttpHeaders().apply { setBearerAuth(tokenA) }),
                String::class.java
            ).statusCode
        )
        val balanceRes = rest.exchange(
            "/accounts/$accountA1",
            HttpMethod.GET,
            HttpEntity<Void>(HttpHeaders().apply { setBearerAuth(tokenA) }),
            LookupOneAccountResponse::class.java
        )
        assertEquals(500, balanceRes.body!!.balance)
    }
}

data class DepositRequest(val amount: Long)
data class WithdrawRequest(val amount: Long)
data class DepositResponse(val accountId: String, val newBalance: Long)
data class WithdrawResponse(val accountId: String, val newBalance: Long)
data class LookupEveryAccountsResponse(val accountIds: List<String>)
data class LookupOneAccountResponse(val balance: Long)
data class OpenAccountResponse(val accountId: String)
data class TransferRequest(val from: String, val to: String, val amount: Long)
