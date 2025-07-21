package com.ywcheong.simple.transaction.certification

import com.ywcheong.simple.transaction.certification.domain.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

/**
 * Certification 도메인 단위 테스트
 */
class CertificationTest {

    private val certificationService = CertificationService()

    @Test
    fun `서명용 개인키로 보증 후 대응되는 공개키로 검증할 수 있다`() {
        // Arrange
        val (privateKey, publicKey) = certificationService.generateKeypair()
        val original = "trust‐but‐verify"
        val message = CertificationMessage(original)

        // Act
        val signed = message.sign(privateKey)
        val verified = signed.verify(publicKey)

        // Assert
        assertEquals(original, verified.value)
    }

    @Test
    fun `서명용 개인키로 보증 후 대응하지 않는 공개키로는 검증할 수 없다`() {
        // Arrange
        val (privateKey, _) = certificationService.generateKeypair()
        val (_, wrongPublicKey) = certificationService.generateKeypair()
        val signed = CertificationMessage("mismatch").sign(privateKey)

        // Act & Assert
        assertThrows<BadSignatureCertificationException> {
            signed.verify(wrongPublicKey)
        }
    }

    @Test
    fun `서명된 메시지가 변조되면 검증에 실패한다 - 메시지 변조`() {
        // Arrange
        val (privateKey, publicKey) = certificationService.generateKeypair()
        val signed = CertificationMessage("immutable").sign(privateKey)
        val tampered = CertificationSignedMessage(signed.value + "hehe")

        // Act & Assert
        assertThrows<BadSignatureCertificationException> {
            tampered.verify(publicKey)
        }
    }

    @Test
    fun `서명이 변조되면 검증에 실패한다`() {
        // Arrange
        val (privateKey, publicKey) = certificationService.generateKeypair()
        val signed = CertificationMessage("immutable").sign(privateKey)
        val tampered = CertificationSignedMessage("hehe" + signed.value)

        // Act & Assert
        assertThrows<BadSignatureCertificationException> {
            tampered.verify(publicKey)
        }
    }

    @Test
    fun `잘못된 형식의 서명 메시지는 검증 전에 거부된다`() {
        // Arrange
        val (_, publicKey) = certificationService.generateKeypair()
        val invalidPayloads = listOf(
            "missing-delimiter",          // 콜론 없음
            "::::",                       // 비정상 구조
            "not-base64:plain-text"       // 서명부가 Base64 아님
        )

        // Act & Assert
        invalidPayloads.forEach { payload ->
            assertThrows<CertificationException> {
                CertificationSignedMessage(payload).verify(publicKey)
            }
        }
    }
}
