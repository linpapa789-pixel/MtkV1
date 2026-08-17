package com.example.protocol

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Native Fastboot Protocol Client implementing USB raw transfer framing.
 * Fastboot commands are ASCII strings followed by single/multiple responses:
 * "INFO<msg>", "OKAY<msg>", "FAIL<msg>", "DATA<hex_len>".
 */
class FastbootProtocolClient(
    private val usbManager: UsbManager,
    private val device: UsbDevice?,
    private val existingConnection: UsbDeviceConnection? = null,
    private val existingInEndpoint: UsbEndpoint? = null,
    private val existingOutEndpoint: UsbEndpoint? = null
) {
    private var connection: UsbDeviceConnection? = existingConnection
    private var fastbootInterface: UsbInterface? = null
    private var inEndpoint: UsbEndpoint? = existingInEndpoint
    private var outEndpoint: UsbEndpoint? = existingOutEndpoint
    private var ownsConnection: Boolean = (existingConnection == null)

    fun isOpen(): Boolean = (connection != null && inEndpoint != null && outEndpoint != null)

    suspend fun open(): Boolean = withContext(Dispatchers.IO) {
        if (isOpen()) return@withContext true

        val dev = device ?: return@withContext false

        try {
            val conn = usbManager.openDevice(dev) ?: return@withContext false
            ownsConnection = true

            // Priority 1: Check standard Fastboot interface descriptor (0xFF, 0x42, 0x03)
            for (i in 0 until dev.interfaceCount) {
                val iface = dev.getInterface(i)
                if (iface.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC &&
                    iface.interfaceSubclass == 0x42 &&
                    iface.interfaceProtocol == 0x03
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
                        fastbootInterface = iface
                        inEndpoint = inEp
                        outEndpoint = outEp
                        return@withContext true
                    }
                }
            }

            // Priority 2: Fallback for devices with generic bulk endpoints in Fastboot mode
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
                    fastbootInterface = iface
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
        if (ownsConnection) {
            try {
                fastbootInterface?.let { connection?.releaseInterface(it) }
                connection?.close()
            } catch (_: Exception) {}
            connection = null
        }
        inEndpoint = null
        outEndpoint = null
        fastbootInterface = null
    }

    /**
     * Executes a fastboot command (e.g. "getvar:all", "flashing unlock", "reboot", "erase:frp")
     */
    suspend fun executeCommand(command: String): FastbootResult = withContext(Dispatchers.IO) {
        val conn = connection ?: return@withContext FastbootResult(false, "", "Device not connected via USB")
        val outEp = outEndpoint ?: return@withContext FastbootResult(false, "", "No USB OUT bulk endpoint")
        val inEp = inEndpoint ?: return@withContext FastbootResult(false, "", "No USB IN bulk endpoint")

        val cmdBytes = command.toByteArray(Charsets.US_ASCII)
        val written = conn.bulkTransfer(outEp, cmdBytes, cmdBytes.size, 3500)
        if (written != cmdBytes.size) {
            return@withContext FastbootResult(false, "", "Failed to send fastboot command over USB")
        }

        val infoMessages = mutableListOf<String>()
        val rxBuf = ByteArray(4096)
        var emptyCount = 0

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 15000L) {
            val read = conn.bulkTransfer(inEp, rxBuf, rxBuf.size, 3000)
            if (read <= 0) {
                emptyCount++
                if (emptyCount > 2) break
                continue
            }
            emptyCount = 0

            val response = String(rxBuf, 0, read, Charsets.US_ASCII)
            val prefix = if (response.length >= 4) response.substring(0, 4) else response
            val payload = if (response.length > 4) response.substring(4) else ""

            when (prefix) {
                "INFO" -> {
                    val clean = payload.trim().removePrefix("(bootloader)").trim()
                    infoMessages.add(clean)
                }
                "OKAY" -> {
                    val clean = payload.trim().removePrefix("(bootloader)").trim()
                    if (clean.isNotBlank()) infoMessages.add(clean)
                    return@withContext FastbootResult(true, infoMessages.joinToString("\n"), "")
                }
                "FAIL" -> {
                    val clean = payload.trim().removePrefix("(bootloader)").trim()
                    return@withContext FastbootResult(false, infoMessages.joinToString("\n"), clean.ifEmpty { "Command execution failed" })
                }
                "DATA" -> {
                    return@withContext FastbootResult(true, "DATA_READY:$payload", "")
                }
                else -> {
                    if (response.contains("OKAY")) {
                        val clean = response.replace("OKAY", "").trim()
                        if (clean.isNotBlank()) infoMessages.add(clean)
                        return@withContext FastbootResult(true, infoMessages.joinToString("\n"), "")
                    } else if (response.contains("FAIL")) {
                        val clean = response.replace("FAIL", "").trim()
                        return@withContext FastbootResult(false, infoMessages.joinToString("\n"), clean)
                    } else {
                        infoMessages.add(response.trim())
                    }
                }
            }
        }
        if (infoMessages.isNotEmpty()) {
            return@withContext FastbootResult(true, infoMessages.joinToString("\n"), "")
        }
        return@withContext FastbootResult(false, "", "Timeout waiting for fastboot response")
    }

    /**
     * Downloads raw partition image payload to device buffer and flashes it to specified partition
     */
    suspend fun downloadAndFlash(
        partition: String,
        payload: ByteArray,
        onProgress: ((Float) -> Unit)? = null
    ): FastbootResult = withContext(Dispatchers.IO) {
        val conn = connection ?: return@withContext FastbootResult(false, "", "Device not connected")
        val outEp = outEndpoint ?: return@withContext FastbootResult(false, "", "No OUT endpoint")
        val inEp = inEndpoint ?: return@withContext FastbootResult(false, "", "No IN endpoint")

        // 1. Send download:<hex_len>
        val hexLen = String.format(Locale.US, "%08x", payload.size)
        val downloadCmd = "download:$hexLen"
        val cmdBytes = downloadCmd.toByteArray(Charsets.US_ASCII)
        val written = conn.bulkTransfer(outEp, cmdBytes, cmdBytes.size, 3500)
        if (written != cmdBytes.size) {
            return@withContext FastbootResult(false, "", "Failed to initiate download command")
        }

        // 2. Expect DATA<hex_len> response
        val rxBuf = ByteArray(1024)
        val read = conn.bulkTransfer(inEp, rxBuf, rxBuf.size, 5000)
        if (read <= 0) {
            return@withContext FastbootResult(false, "", "Device did not respond to download command")
        }
        val resp = String(rxBuf, 0, read, Charsets.US_ASCII)
        if (!resp.startsWith("DATA")) {
            return@withContext FastbootResult(false, "", "Unexpected response before data transfer: $resp")
        }

        // 3. Send payload in 64KB chunks with live progress tracking
        val chunkSize = 64 * 1024
        var offset = 0
        val total = payload.size

        while (offset < total) {
            val chunkLen = minOf(chunkSize, total - offset)
            val chunkBytes = payload.copyOfRange(offset, offset + chunkLen)
            val transfered = conn.bulkTransfer(outEp, chunkBytes, chunkBytes.size, 8000)
            if (transfered < 0) {
                return@withContext FastbootResult(false, "", "Data transfer error at byte $offset")
            }
            offset += transfered
            val progress = offset.toFloat() / total.toFloat()
            onProgress?.invoke(progress)
        }

        // 4. Expect OKAY after download
        val okayRead = conn.bulkTransfer(inEp, rxBuf, rxBuf.size, 5000)
        if (okayRead <= 0 || !String(rxBuf, 0, okayRead, Charsets.US_ASCII).contains("OKAY")) {
            return@withContext FastbootResult(false, "", "Download buffer acknowledgment failed")
        }

        // 5. Send flash:<partition>
        val flashCmd = "flash:$partition"
        return@withContext executeCommand(flashCmd)
    }
}

data class FastbootResult(
    val isSuccess: Boolean,
    val info: String,
    val error: String
)

