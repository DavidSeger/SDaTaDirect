package unibas.dmi.sdatadirect.crypto

import java.security.*
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class CryptoHandler {

    private val ALGORITHM: String = "AES"
    private val cipherAES: Cipher = Cipher.getInstance("AES")
    private val cipherRSA: Cipher = Cipher.getInstance("RSA")

    var sharedAESKey: SecretKey? = null
    var publicRSAKey: PublicKey? = null
    var privateRSAKey: PrivateKey? = null


    fun keyAESGenerator(): SecretKey {
        val kgAES: KeyGenerator = KeyGenerator.getInstance("AES")
        kgAES.init(128)

        return kgAES.generateKey()
    }

    fun keyPairRSAGenerator(): KeyPair {
        val kgRSA: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
        kgRSA.initialize(1024)

        return kgRSA.genKeyPair()
    }

    fun getSecretKeyEncoded(secKey: SecretKey): String {

        return Base64.getEncoder().encodeToString(secKey.encoded)
    }

    fun getSecretKeyDecoded(secKeyEncoded: String): SecretKey {
        val decodedKey: ByteArray = Base64.getDecoder().decode(secKeyEncoded)
        return SecretKeySpec(decodedKey, 0, decodedKey.size, "AES")
    }

    fun getPublicKeyEncoded(pubKey: PublicKey): String {

        return Base64.getEncoder().encodeToString(pubKey.encoded)
    }

    fun getPublicKeyDecoded(pubKeyEncoded: String): PublicKey {
        //val decodedKey: ByteArray = Base64.getDecoder().decode()
        //return SecretKeySpec(decodedKey, 0, decodedKey.size, "RSA")
        val keyBytes: ByteArray = Base64.getDecoder().decode(pubKeyEncoded)
        val spec: X509EncodedKeySpec = X509EncodedKeySpec(keyBytes)
        val keyFactor: KeyFactory = KeyFactory.getInstance("RSA")
        val key = keyFactor.generatePublic(spec)

        return key
    }

    fun getPrivateKeyEncoded(privKey: PrivateKey): String {

        return Base64.getEncoder().encodeToString(privKey.encoded)
    }

    fun getPrivateKeyDecoded(privKeyEncoded: String): PrivateKey {
        val keyBytes: ByteArray = Base64.getDecoder().decode(privKeyEncoded)
        val spec: X509EncodedKeySpec = X509EncodedKeySpec(keyBytes)
        val keyFactor: KeyFactory = KeyFactory.getInstance("RSA")
        val key = keyFactor.generatePrivate(spec)

        return key
    }

    fun encryptAES(message: ByteArray, key: SecretKey): ByteArray {
        cipherAES.init(Cipher.ENCRYPT_MODE, key)
        return cipherAES.doFinal(message)
    }

    fun decryptAES(encryptedMessage: ByteArray, key: SecretKey): ByteArray {
        cipherAES.init(Cipher.DECRYPT_MODE, key)
        return cipherAES.doFinal(encryptedMessage)
    }

    fun encryptRSA(message: ByteArray, key: SecretKey): ByteArray {
        cipherRSA.init(Cipher.ENCRYPT_MODE, key)
        return cipherRSA.doFinal(message)
    }

    fun decryptRSA(encryptedMessage: ByteArray, key: SecretKey): ByteArray {
        cipherRSA.init(Cipher.DECRYPT_MODE, key)
        return cipherRSA.doFinal(encryptedMessage)
    }

    fun createSignature(message: ByteArray): ByteArray {
        val signatureAlgorithm: Signature = Signature.getInstance("SHA256WithRSA")
        signatureAlgorithm.initSign(privateRSAKey)
        signatureAlgorithm.update(message)
        val signature: ByteArray = signatureAlgorithm.sign()

        return signature
    }

    fun verifySignature(signature: ByteArray): Boolean {
        val verificationAlgorithm: Signature = Signature.getInstance("SHA256WithRSA")
        verificationAlgorithm.initVerify(publicRSAKey)
        verificationAlgorithm.update(signature)
        val match = verificationAlgorithm.verify(signature)

        return match
    }
}
