package com.example.protocol

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    suspend fun open(): Boolean = withContext(Dispatchers.IO) {
        if (connection != null && inEndpoint != null && outEndpoint != null) {
            return@withContext true
        }

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
    }

    /**
     * Executes a fastboot command (e.g. "getvar:all", "flashing unlock", "reboot")
     */
    suspend fun executeCommand(command: String): FastbootResult = withContext(Dispatchers.IO) {
        val conn = connection ?: return@withContext FastbootResult(false, "", "Device not connected")
        val outEp = outEndpoint ?: return@withContext FastbootResult(false, "", "No OUT endpoint")
        val inEp = inEndpoint ?: return@withContext FastbootResult(false, "", "No IN endpoint")

        val cmdBytes = command.toByteArray(Charsets.US_ASCII)
        val written = conn.bulkTransfer(outEp, cmdBytes, cmdBytes.size, 3000)
        if (written != cmdBytes.size) {
            return@withContext FastbootResult(false, "", "Failed to write fastboot command")
        }

        val infoMessages = mutableListOf<String>()
        val rxBuf = ByteArray(1024)

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 10000L) {
            val read = conn.bulkTransfer(inEp, rxBuf, rxBuf.size, 4000)
            if (read <= 0) break

            val response = String(rxBuf, 0, read, Charsets.US_ASCII)
            val prefix = if (response.length >= 4) response.substring(0, 4) else response
            val payload = if (response.length > 4) response.substring(4) else ""

            when (prefix) {
                "INFO" -> {
                    infoMessages.add(payload.trim())
                }
                "OKAY" -> {
                    return@withContext FastbootResult(true, infoMessages.joinToString("\n") + (if (payload.isNotBlank()) "\n$payload" else ""), "")
                }
                "FAIL" -> {
                    return@withContext FastbootResult(false, infoMessages.joinToString("\n"), payload.trim())
                }
                "DATA" -> {
                    return@withContext FastbootResult(true, "DATA_READY:$payload", "")
                }
                else -> {
                    // Sometimes fastboot output comes with newlines or mixed packets
                    if (response.contains("OKAY")) {
                        return@withContext FastbootResult(true, infoMessages.joinToString("\n") + "\n" + response.trim(), "")
                    } else if (response.contains("FAIL")) {
                        return@withContext FastbootResult(false, infoMessages.joinToString("\n"), response.trim())
                    } else {
                        infoMessages.add(response.trim())
                    }
                }
            }
        }
        return@withContext FastbootResult(false, infoMessages.joinToString("\n"), "Timeout waiting for fastboot response")
    }
}

data class FastbootResult(
    val isSuccess: Boolean,
    val info: String,
    val error: String
)
