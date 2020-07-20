package unibas.dmi.sdatadirect.crypto

import java.io.File
import java.io.FileInputStream
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class CryptoHandler {

    var kgAES: KeyGenerator
    lateinit var kgRSA: KeyGenerator
    var secKey: SecretKey
    lateinit var cipherAES: Cipher

    init {
        kgAES = KeyGenerator.getInstance("AES")
        kgAES.init(128)
        secKey = kgAES.generateKey()
        cipherAES = Cipher.getInstance("AES")
    }

    fun encrypt(filePath: String, key: Key) {
        val fileInputStream = FileInputStream(File(filePath))
        val fileToByteArray: ByteArray = fileInputStream.readBytes()

        cipherAES.init(Cipher.ENCRYPT_MODE, key)

        val encryptedFile: ByteArray = cipherAES.doFinal(fileToByteArray)
    }

    fun decrypt(encryptedFile: ByteArray, key: Key) {

    }


}