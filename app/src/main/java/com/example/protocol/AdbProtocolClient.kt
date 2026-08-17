package com.example.protocol

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.util.Base64
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Arrays
import javax.crypto.Cipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Standard Android ADB Protocol Implementation with full A_AUTH RSA handshake support.
 * Format: [Command (4B)][Arg0 (4B)][Arg1 (4B)][DataLength (4B)][DataChecksum (4B)][Magic (4B)]
 */
class AdbProtocolClient(
    private val usbManager: UsbManager,
    private val device: UsbDevice?,
    private val existingConnection: UsbDeviceConnection? = null,
    private val existingInEndpoint: UsbEndpoint? = null,
    private val existingOutEndpoint: UsbEndpoint? = null
) {
    companion object {
        const val A_SYNC = 0x434e5953
        const val A_CNXN = 0x4e584e43
        const val A_OPEN = 0x4e45504f
        const val A_OKAY = 0x59414b4f
        const val A_CLSE = 0x45534c43
        const val A_WRTE = 0x45545257
        const val A_AUTH = 0x48545541

        const val ADB_AUTH_TOKEN = 1
        const val ADB_AUTH_SIGNATURE = 2
        const val ADB_AUTH_RSAPUBLICKEY = 3

        const val ADB_VERSION = 0x01000000
        const val MAX_PAYLOAD = 4096

        private const val PREF_NAME = "mtk_adb_keys"
        private const val KEY_PRIV = "adb_private_key"
        private const val KEY_PUB = "adb_public_key"

        fun getOrCreateKeyPair(context: Context): KeyPair {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val privStr = prefs.getString(KEY_PRIV, null)
            val pubStr = prefs.getString(KEY_PUB, null)

            if (privStr != null && pubStr != null) {
                try {
                    val kf = KeyFactory.getInstance("RSA")
                    val privBytes = Base64.decode(privStr, Base64.DEFAULT)
                    val pubBytes = Base64.decode(pubStr, Base64.DEFAULT)
                    val privKey = kf.generatePrivate(PKCS8EncodedKeySpec(privBytes))
                    val pubKey = kf.generatePublic(X509EncodedKeySpec(pubBytes))
                    return KeyPair(pubKey, privKey)
                } catch (_: Exception) {}
            }

            val kpg = KeyPairGenerator.getInstance("RSA")
            kpg.initialize(2048)
            val kp = kpg.generateKeyPair()

            prefs.edit()
                .putString(KEY_PRIV, Base64.encodeToString(kp.private.encoded, Base64.NO_WRAP))
                .putString(KEY_PUB, Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP))
                .apply()

            return kp
        }

        fun signToken(token: ByteArray, privateKey: RSAPrivateKey): ByteArray {
            val cipher = Cipher.getInstance("RSA/ECB/NoPadding")
            val block = ByteArray(256)
            block[0] = 0x00
            block[1] = 0x01
            
            val sha1Prefix = byteArrayOf(
                0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2B, 0x0E, 0x03, 0x02, 0x1A, 0x05, 0x00, 0x04, 0x14
            )
            
            val padLen: Int
            if (token.size == 20) {
                padLen = 256 - 3 - sha1Prefix.size - token.size
                Arrays.fill(block, 2, 2 + padLen, 0xFF.toByte())
                block[2 + padLen] = 0x00
                System.arraycopy(sha1Prefix, 0, block, 2 + padLen + 1, sha1Prefix.size)
                System.arraycopy(token, 0, block, 2 + padLen + 1 + sha1Prefix.size, token.size)
            } else {
                padLen = 256 - 3 - token.size
                Arrays.fill(block, 2, 2 + padLen, 0xFF.toByte())
                block[2 + padLen] = 0x00
                System.arraycopy(token, 0, block, 2 + padLen + 1, token.size)
            }

            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            return cipher.doFinal(block)
        }

        fun getAdbPublicKeyPayload(publicKey: RSAPublicKey): ByteArray {
            val n = publicKey.modulus
            val r = BigInteger.ONE.shiftLeft(2048)
            val rr = r.multiply(r).mod(n)

            val base32 = BigInteger.valueOf(2).pow(32)
            val n0 = n.remainder(base32)
            val n0inv = base32.subtract(n0.modInverse(base32)).remainder(base32).toInt()

            val buffer = ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN)
            buffer.putInt(64) // 2048 / 32 = 64 32-bit words
            buffer.putInt(n0inv)

            for (i in 0 until 64) {
                val word = n.shiftRight(i * 32).remainder(base32).toLong().toInt()
                buffer.putInt(word)
            }

            for (i in 0 until 64) {
                val word = rr.shiftRight(i * 32).remainder(base32).toLong().toInt()
                buffer.putInt(word)
            }

            buffer.putInt(publicKey.publicExponent.toInt())

            val b64 = Base64.encodeToString(buffer.array(), Base64.NO_WRAP)
            val adbKeyString = "$b64 MTKUnlockTool@Android\u0000"
            return adbKeyString.toByteArray(Charsets.UTF_8)
        }
    }

    private var connection: UsbDeviceConnection? = existingConnection
    private var adbInterface: UsbInterface? = null
    private var inEndpoint: UsbEndpoint? = existingInEndpoint
    private var outEndpoint: UsbEndpoint? = existingOutEndpoint
    private var ownsConnection: Boolean = (existingConnection == null)
    private var localIdCounter = 1

    var isConnected: Boolean = false
        private set

    fun isOpen(): Boolean = (connection != null && inEndpoint != null && outEndpoint != null)

    suspend fun open(): Boolean = withContext(Dispatchers.IO) {
        if (isOpen()) return@withContext true
        val dev = device ?: return@withContext false

        try {
            val conn = usbManager.openDevice(dev) ?: return@withContext false
            ownsConnection = true
            
            // Priority 1: Match standard ADB Interface class (0xFF, 0x42, 0x01)
            for (i in 0 until dev.interfaceCount) {
                val iface = dev.getInterface(i)
                if (iface.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC &&
                    iface.interfaceSubclass == 0x42 &&
                    iface.interfaceProtocol == 0x01
                ) {
                    var inEp: UsbEndpoint? = null
                    var outEp: UsbEndpoint? = null
                    for (j in 0 until iface.endpointCount) {
                        val ep = iface.getEndpoint(j)
                        if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                            if (ep.direction == UsbConstants.USB_DIR_IN) inEp = ep
                            else outEp = ep
                        }
                    }
                    if (inEp != null && outEp != null) {
                        conn.claimInterface(iface, true)
                        connection = conn
                        adbInterface = iface
                        inEndpoint = inEp
                        outEndpoint = outEp
                        return@withContext true
                    }
                }
            }

            // Priority 2: Generic Bulk Interface fallback
            for (i in 0 until dev.interfaceCount) {
                val iface = dev.getInterface(i)
                var inEp: UsbEndpoint? = null
                var outEp: UsbEndpoint? = null
                for (j in 0 until iface.endpointCount) {
                    val ep = iface.getEndpoint(j)
                    if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (ep.direction == UsbConstants.USB_DIR_IN) inEp = ep
                        else outEp = ep
                    }
                }
                if (inEp != null && outEp != null) {
                    conn.claimInterface(iface, true)
                    connection = conn
                    adbInterface = iface
                    inEndpoint = inEp
                    outEndpoint = outEp
                    return@withContext true
                }
            }

            conn.close()
            return@withContext false
        } catch (_: Exception) {
            return@withContext false
        }
    }

    fun close() {
        isConnected = false
        if (ownsConnection) {
            try {
                adbInterface?.let { connection?.releaseInterface(it) }
                connection?.close()
            } catch (_: Exception) {}
            connection = null
        }
        inEndpoint = null
        outEndpoint = null
        adbInterface = null
    }

    private fun sendPacket(cmd: Int, arg0: Int, arg1: Int, data: ByteArray? = null): Boolean {
        val conn = connection ?: return false
        val outEp = outEndpoint ?: return false

        val dataLen = data?.size ?: 0
        var checksum = 0
        if (data != null) {
            for (b in data) {
                checksum += (b.toInt() and 0xFF)
            }
        }
        val magic = cmd xor -0x1

        val header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
        header.putInt(cmd)
        header.putInt(arg0)
        header.putInt(arg1)
        header.putInt(dataLen)
        header.putInt(checksum)
        header.putInt(magic)

        val headerBytes = header.array()
        val hWritten = conn.bulkTransfer(outEp, headerBytes, headerBytes.size, 2500)
        if (hWritten != 24) return false

        if (data != null && data.isNotEmpty()) {
            val dWritten = conn.bulkTransfer(outEp, data, data.size, 3500)
            if (dWritten != data.size) return false
        }
        return true
    }

    private fun readPacket(timeout: Int = 3000): Pair<IntArray, ByteArray?>? {
        val conn = connection ?: return null
        val inEp = inEndpoint ?: return null

        val headerBuf = ByteArray(24)
        val hRead = conn.bulkTransfer(inEp, headerBuf, 24, timeout)
        if (hRead != 24) return null

        val bb = ByteBuffer.wrap(headerBuf).order(ByteOrder.LITTLE_ENDIAN)
        val cmd = bb.int
        val arg0 = bb.int
        val arg1 = bb.int
        val dataLen = bb.int
        val checksum = bb.int
        val magic = bb.int

        if ((cmd xor -0x1) != magic) return null

        var data: ByteArray? = null
        if (dataLen > 0) {
            data = ByteArray(dataLen)
            var totalRead = 0
            while (totalRead < dataLen) {
                val chunkSize = (dataLen - totalRead).coerceAtMost(MAX_PAYLOAD)
                val tempBuf = ByteArray(chunkSize)
                val read = conn.bulkTransfer(inEp, tempBuf, chunkSize, timeout)
                if (read <= 0) break
                System.arraycopy(tempBuf, 0, data, totalRead, read)
                totalRead += read
            }
        }
        return Pair(intArrayOf(cmd, arg0, arg1, dataLen, checksum, magic), data)
    }

    suspend fun connect(context: Context, onAuthPrompt: (() -> Unit)? = null): Boolean = withContext(Dispatchers.IO) {
        if (isConnected) return@withContext true

        val banner = "host::MTKUnlockTool\u0000".toByteArray(Charsets.UTF_8)
        if (!sendPacket(A_CNXN, ADB_VERSION, MAX_PAYLOAD, banner)) {
            isConnected = false
            return@withContext false
        }
        var response = readPacket(3000) ?: run {
            isConnected = false
            return@withContext false
        }
        var cmd = response.first[0]

        if (cmd == A_CNXN) {
            isConnected = true
            return@withContext true
        }

        if (cmd == A_AUTH) {
            val token = response.second ?: run {
                isConnected = false
                return@withContext false
            }
            val keyPair = getOrCreateKeyPair(context)
            val privKey = keyPair.private as RSAPrivateKey
            val pubKey = keyPair.public as RSAPublicKey

            // 1. Try to sign with private key in case device was previously authorized
            try {
                val signature = signToken(token, privKey)
                if (sendPacket(A_AUTH, ADB_AUTH_SIGNATURE, 0, signature)) {
                    val authResp = readPacket(1500)
                    if (authResp != null && authResp.first[0] == A_CNXN) {
                        isConnected = true
                        return@withContext true
                    }
                }
            } catch (_: Exception) {}

            // 2. Not authorized yet: Send RSAPUBLICKEY to prompt target phone screen
            val pubPayload = getAdbPublicKeyPayload(pubKey)
            if (!sendPacket(A_AUTH, ADB_AUTH_RSAPUBLICKEY, 0, pubPayload)) {
                isConnected = false
                return@withContext false
            }

            onAuthPrompt?.invoke()

            // 3. Poll and wait up to 20 seconds for user to tap "Allow" on target phone screen
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 20000L) {
                val authWait = readPacket(2000)
                if (authWait != null) {
                    if (authWait.first[0] == A_CNXN) {
                        isConnected = true
                        return@withContext true
                    }
                    if (authWait.first[0] == A_AUTH && authWait.second != null) {
                        // Resend signature if challenged again
                        try {
                            val sig = signToken(authWait.second!!, privKey)
                            sendPacket(A_AUTH, ADB_AUTH_SIGNATURE, 0, sig)
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        isConnected = false
        return@withContext false
    }

    suspend fun executeShell(command: String): String = withContext(Dispatchers.IO) {
        val localId = localIdCounter++
        val dest = "shell:$command\u0000".toByteArray(Charsets.UTF_8)
        if (!sendPacket(A_OPEN, localId, 0, dest)) {
            isConnected = false
            return@withContext "ERROR: Failed to send A_OPEN to ADB target"
        }

        var remoteId = 0
        val sb = StringBuilder()

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 15000L) {
            val packet = readPacket(2500) ?: break
            val cmd = packet.first[0]
            val arg0 = packet.first[1]
            val arg1 = packet.first[2]
            val payload = packet.second

            when (cmd) {
                A_OKAY -> {
                    remoteId = arg0
                }
                A_WRTE -> {
                    if (payload != null) {
                        sb.append(String(payload, Charsets.UTF_8))
                    }
                    sendPacket(A_OKAY, localId, remoteId)
                }
                A_CLSE -> {
                    sendPacket(A_CLSE, localId, remoteId)
                    break
                }
            }
        }
        return@withContext sb.toString().trim()
    }
}
