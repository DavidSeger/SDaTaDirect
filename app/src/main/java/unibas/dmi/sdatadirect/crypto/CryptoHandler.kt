package unibas.dmi.sdatadirect.crypto

import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * This class contains all functions for encoding, decoding, encryption, decryption, signature and
 * verification. The RSA and AES cryptographic algorithms are implemented.
 */

class CryptoHandler {

    private val cipherAES: Cipher = Cipher.getInstance("AES")
    private val cipherRSA: Cipher = Cipher.getInstance("RSA")

    var secretAESKey: SecretKey? = null
    var publicRSAKey: PublicKey? = null
    var privateRSAKey: PrivateKey? = null


    /**
     * Generates ARS symmetric key
     */
    fun keyAESGenerator(): SecretKey {
        val kgAES: KeyGenerator = KeyGenerator.getInstance("AES")
        kgAES.init(128)

        return kgAES.generateKey()
    }

    /**
     * Generates RSA asymmetric key pair
     */
    fun keyPairRSAGenerator(): KeyPair {
        val kgRSA: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
        kgRSA.initialize(1024)

        return kgRSA.genKeyPair()
    }

    /**
     * Encoded a SecretKey instance into a string
     */
    fun getSecretKeyEncoded(secKey: SecretKey): String {

        return Base64.getEncoder().encodeToString(secKey.encoded)
    }

    /**
     * Decodes a string back to SecretKey
     */
    fun getSecretKeyDecoded(secKeyEncoded: String): SecretKey {
        val decodedKey: ByteArray = Base64.getDecoder().decode(secKeyEncoded)
        return SecretKeySpec(decodedKey, 0, decodedKey.size, "AES")
    }

    /**
     * Encoded a PublicKey instance into a string
     */
    fun getPublicKeyEncoded(pubKey: PublicKey): String {

        return Base64.getEncoder().encodeToString(pubKey.encoded)
    }

    /**
     * Decodes a string back to PublicKey
     */
    fun getPublicKeyDecoded(pubKeyEncoded: String): PublicKey {
        val keyBytes: ByteArray = Base64.getDecoder().decode(pubKeyEncoded)
        val spec: X509EncodedKeySpec = X509EncodedKeySpec(keyBytes)
        val keyFactor: KeyFactory = KeyFactory.getInstance("RSA")
        val key = keyFactor.generatePublic(spec)

        return key
    }

    /**
     * Encoded a PrivateKey instance into a string
     */
    fun getPrivateKeyEncoded(privKey: PrivateKey): String {

        return Base64.getEncoder().encodeToString(privKey.encoded)
    }

    /**
     * Decodes a string back to PrivateKey
     */
    fun getPrivateKeyDecoded(privKeyEncoded: String): PrivateKey {
        val keyBytes: ByteArray = Base64.getDecoder().decode(privKeyEncoded)
        //val spec: X509EncodedKeySpec = X509EncodedKeySpec(keyBytes)
        val spec: PKCS8EncodedKeySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactor: KeyFactory = KeyFactory.getInstance("RSA")
        val key = keyFactor.generatePrivate(spec)

        return key
    }

    /**
     * Encrypts a message ByteArray into an encrypted ByteArray using Cipher class with AES algorithm
     */
    fun encryptAES(message: ByteArray, key: String?): ByteArray {
        val secretKey = getSecretKeyDecoded(key!!)
        cipherAES.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipherAES.doFinal(message)
    }

    /**
     * Decrypts an encrypted ByteArray into a plain ByteArray using Cipher class with AES algorithm
     */
    fun decryptAES(encryptedMessage: ByteArray, key: String?): ByteArray {
        val secretKey = getSecretKeyDecoded(key!!)
        cipherAES.init(Cipher.DECRYPT_MODE, secretKey)
        return cipherAES.doFinal(encryptedMessage)
    }

    /**
     * Encrypts a message ByteArray into an encrypted ByteArray using Cipher class with RSA algorithm
     * and a RSA public key
     */
    fun encryptRSA(message: ByteArray, key: PublicKey): ByteArray {
        cipherRSA.init(Cipher.ENCRYPT_MODE, key)
        return cipherRSA.doFinal(message)
    }

    /**
     * Decrypts an encrypted ByteArray into a plain ByteArray using Cipher class with RSA algorithm
     * and a RSA private key
     */
    fun decryptRSA(encryptedMessage: ByteArray, key: PrivateKey): ByteArray {
        cipherRSA.init(Cipher.DECRYPT_MODE, key)
        return cipherRSA.doFinal(encryptedMessage)
    }

    /**
     * Creates a signature of a message ByteArray using Signature class with SHA256WithRSA algorithm
     * and RSA private key
     */
    fun createSignature(message: ByteArray, privateKey: String?): ByteArray {
        val signatureAlgorithm: Signature = Signature.getInstance("SHA256WithRSA")
        val privateKeyRSA = getPrivateKeyDecoded(privateKey!!)
        signatureAlgorithm.initSign(privateKeyRSA)
        signatureAlgorithm.update(message)
        val signature: ByteArray = signatureAlgorithm.sign()

        return signature
    }

    /**
     * Verifies the signature using Signature class with SHA256WithRSA and RSA public key
     */
    fun verifySignature(signature: ByteArray, message: ByteArray, publicKey: String?): Boolean {
        val verificationAlgorithm: Signature = Signature.getInstance("SHA256WithRSA")
        val publicKeyRSA = getPublicKeyDecoded(publicKey!!)
        verificationAlgorithm.initVerify(publicKeyRSA)
        verificationAlgorithm.update(message)
        val match = verificationAlgorithm.verify(signature)

        return match
    }
}
