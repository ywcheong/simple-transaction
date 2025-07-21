package com.ywcheong.simple.transaction.member

import com.ywcheong.simple.transaction.member.infra.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*
import org.springframework.jdbc.core.JdbcTemplate

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 운영 DB 설정 사용
class MemberIntegrationTest @Autowired constructor(
    private val rest: TestRestTemplate, private val jdbc: JdbcTemplate
) {

    /* ---------- 공통 유틸 ---------- */

    private val defaultId = "testuser"
    private val defaultPw = "P@ssw0rd!"
    private val defaultName = "테스트유저"
    private val defaultPhone = "+82-10-1234-5678"

    private fun register(
        id: String = defaultId, pw: String = defaultPw
    ): ResponseEntity<RegisterResponse> = rest.postForEntity(
        "/members", RegisterRequest(id, defaultName, pw, defaultPhone), RegisterResponse::class.java
    )

    private fun registerNoParse(
        id: String = defaultId, pw: String = defaultPw
    ): ResponseEntity<String> = rest.postForEntity(
        "/members", RegisterRequest(id, defaultName, pw, defaultPhone), String::class.java
    )

    private fun issueToken(
        id: String = defaultId, pw: String = defaultPw
    ): ResponseEntity<TokenResponse> = rest.postForEntity(
        "/members/tokens", TokenRequest(id, pw), TokenResponse::class.java
    )

    private fun issueTokenNoParse(
        id: String = defaultId, pw: String = defaultPw
    ): ResponseEntity<String> = rest.postForEntity(
        "/members/tokens", TokenRequest(id, pw), String::class.java
    )

    private fun withdraw(token: String): ResponseEntity<WithdrawResponse> {
        val headers = HttpHeaders().apply { setBearerAuth(token) }
        return rest.exchange(
            "/members", HttpMethod.DELETE, HttpEntity<Void>(headers), WithdrawResponse::class.java
        )
    }

    private fun withdrawNoParse(token: String): ResponseEntity<String> {
        val headers = HttpHeaders().apply { setBearerAuth(token) }
        return rest.exchange(
            "/members", HttpMethod.DELETE, HttpEntity<Void>(headers), String::class.java
        )
    }

    private fun withdrawNoParseNoAuth(): ResponseEntity<String> {
        val headers = HttpHeaders()
        return rest.exchange(
            "/members", HttpMethod.DELETE, HttpEntity<Void>(headers), String::class.java
        )
    }

    @BeforeEach
    fun cleanDatabase() {
        jdbc.execute("DELETE FROM member WHERE TRUE")                // 테이블명은 실제 스키마에 맞게 수정
    }

    @Test
    fun `가입 후 로그인 후 탈퇴할 수 있다`() {
        // Arrange
        assertEquals(HttpStatus.OK, register().statusCode)
        val token = issueToken().body!!.token

        // Act
        val firstWithdraw = withdraw(token)

        // Assert
        assertEquals(HttpStatus.OK, firstWithdraw.statusCode)
    }

    @Test
    fun `두 번 탈퇴할 수 없다`() {
        // Arrange
        assertEquals(HttpStatus.OK, register().statusCode)
        val token = issueToken().body!!.token

        // Act
        val firstWithdraw = withdraw(token)
        val secondWithdraw = withdrawNoParse(token)              // 이미 탈퇴된 계정 재탈퇴 시도

        // Assert
        assertEquals(HttpStatus.OK, firstWithdraw.statusCode)
        assertEquals(HttpStatus.BAD_REQUEST, secondWithdraw.statusCode)
    }

    @Test
    fun `두 번 가입할 수 없다`() {
        // Arrange
        assertEquals(HttpStatus.OK, register().statusCode)

        // Act
        val duplicate = registerNoParse()

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, duplicate.statusCode)
    }

    @Test
    fun `가입하지 않고 토큰을 발급받을 수 없다`() {
        // Act
        val noUserToken = issueTokenNoParse()

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, noUserToken.statusCode)
    }

    @Test
    fun `잘못된 비밀번호로 토큰을 발급받을 수 없다`() {
        // Arrange
        register()

        // Act
        val wrongPwToken = issueTokenNoParse(pw = "WrongPassword")

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, wrongPwToken.statusCode)
    }

    @Test
    fun `변조된 토큰을 사용할 수 없다`() {
        // Arrange
        register()
        val originalToken = issueToken().body!!.token
        val tamperedToken = originalToken.dropLast(1) + "x" // 토큰 일부 변조

        // Act
        val response = withdraw(tamperedToken)

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `가입하지 않고 탈퇴할 수 없다`() {
        // Act
        val response = withdrawNoParseNoAuth()

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }
}
