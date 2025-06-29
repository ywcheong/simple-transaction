package com.ywcheong.simple.transaction.security.jwt

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

@Component
@ConfigurationProperties(prefix = "st.secret")
class JwtKeyProperties {
    lateinit var jwtPubkey: String
    lateinit var jwtPrvkey: String
}

@Component
class JwtKeyProvider(
    private val properties: JwtKeyProperties
) {
    val publicKey: PublicKey by lazy { loadPublicKey(properties.jwtPubkey) }
    val privateKey: PrivateKey by lazy { loadPrivateKey(properties.jwtPrvkey) }

    private fun loadPublicKey(pem: String): PublicKey {
        val publicKeyPEM = pem.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")
        val decoded = Base64.getDecoder().decode(publicKeyPEM)
        val keySpec = X509EncodedKeySpec(decoded)
        return KeyFactory.getInstance("RSA").generatePublic(keySpec)
    }

    private fun loadPrivateKey(pem: String): PrivateKey {
        val privateKeyPEM = pem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val decoded = Base64.getDecoder().decode(privateKeyPEM)
        val keySpec = PKCS8EncodedKeySpec(decoded)
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec)
    }
}
