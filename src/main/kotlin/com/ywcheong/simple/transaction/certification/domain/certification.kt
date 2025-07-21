package com.ywcheong.simple.transaction.certification.domain

import org.springframework.stereotype.Service
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

@JvmInline
value class CertificationPublicKey(val value: String) {
    internal fun toJavaPublicKey(): PublicKey {
        val bytes = Base64.getDecoder().decode(value)
        val spec = X509EncodedKeySpec(bytes)
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }

    companion object {
        internal fun fromJavaKey(key: PublicKey): CertificationPublicKey {
            val encoded = key.encoded
            val b64 = Base64.getEncoder().encodeToString(encoded)
            return CertificationPublicKey(b64)
        }
    }
}

@JvmInline
value class CertificationPrivateKey(val value: String) {
    internal fun toJavaPrivateKey(): PrivateKey {
        val bytes = Base64.getDecoder().decode(value)
        val spec = PKCS8EncodedKeySpec(bytes)
        return KeyFactory.getInstance("RSA").generatePrivate(spec)
    }

    companion object {
        internal fun fromJavaKey(key: PrivateKey): CertificationPrivateKey {
            val encoded = key.encoded
            val b64 = Base64.getEncoder().encodeToString(encoded)
            return CertificationPrivateKey(b64)
        }
    }
}

@JvmInline
value class CertificationMessage(val value: String) {
    fun sign(privateKey: CertificationPrivateKey): CertificationSignedMessage {
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey.toJavaPrivateKey())
        signature.update(value.toByteArray(Charsets.UTF_8))
        val signedBytes = signature.sign()
        val signedBase64 = Base64.getEncoder().encodeToString(signedBytes)
        return CertificationSignedMessage("$signedBase64:$value")
    }
}

@JvmInline
value class CertificationSignedMessage(val value: String) {
    fun verify(publicKey: CertificationPublicKey): CertificationMessage {
        // CertificationMessage.sign 함수의 양식에 따라 $signedBase64:$value 꼴의 페이로드를 가정함
        val parts = value.split(":", limit = 2)
        if (parts.size != 2) throw BadFormatCertificationException()

        val (sigB64, msg) = parts
        val sigBytes = try {
            Base64.getDecoder().decode(sigB64)
        } catch (_: IllegalArgumentException) {
            throw BadFormatCertificationException()
        }

        val valid = try {
            Signature.getInstance("SHA256withRSA").apply {
                initVerify(publicKey.toJavaPublicKey())
                update(msg.toByteArray(Charsets.UTF_8))
            }.verify(sigBytes)
        } catch (_: SignatureException) {
            throw BadSignatureCertificationException()
        }

        if (!valid) throw BadSignatureCertificationException()
        return CertificationMessage(msg)
    }
}

@Service
class CertificationService {
    fun generateKeypair(): Pair<CertificationPrivateKey, CertificationPublicKey> {
        val keyPairGen = KeyPairGenerator.getInstance("RSA")
        keyPairGen.initialize(2048)
        val keyPair = keyPairGen.generateKeyPair()
        return Pair(
            CertificationPrivateKey.fromJavaKey(keyPair.private), CertificationPublicKey.fromJavaKey(keyPair.public)
        )
    }
}
