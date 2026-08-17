package com.example.parser

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Android Sparse Image Parser (Header Magic: 0xED26FF3A)
 * Directly matches MTKclient / fastboot sparse image flashing engine.
 * Allows high-speed flashing by skipping "Don't Care" empty blocks (Speedup 5x to 10x).
 */
data class SparseHeader(
    val magic: Int,          // 0xED26FF3A
    val majorVersion: Short, // 1
    val minorVersion: Short, // 0
    val fileHeaderSize: Short, // 28 bytes
    val chunkHeaderSize: Short, // 12 bytes
    val blockSize: Int,      // Block size in bytes (usually 4096)
    val totalBlocks: Int,    // Total blocks in output image
    val totalChunks: Int,    // Total chunks in sparse image
    val imageChecksum: Int   // CRC32 checksum
)

sealed class SparseChunk {
    data class Raw(val blockCount: Int, val totalBytes: Long, val fileOffset: Long) : SparseChunk()
    data class Fill(val blockCount: Int, val totalBytes: Long, val fillPattern: ByteArray) : SparseChunk()
    data class DontCare(val blockCount: Int, val totalBytes: Long) : SparseChunk()
    data class Crc32(val crc: Int) : SparseChunk()
}

object SparseImageParser {
    const val SPARSE_HEADER_MAGIC = 0xED26FF3A.toInt()
    const val CHUNK_TYPE_RAW = 0xCAC1.toShort()
    const val CHUNK_TYPE_FILL = 0xCAC2.toShort()
    const val CHUNK_TYPE_DONT_CARE = 0xCAC3.toShort()
    const val CHUNK_TYPE_CRC32 = 0xCAC4.toShort()

    fun isSparseImage(file: File): Boolean {
        if (!file.exists() || file.length() < 28) return false
        return try {
            FileInputStream(file).use { input ->
                val buf = ByteArray(4)
                if (input.read(buf) != 4) return false
                val magic = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).int
                magic == SPARSE_HEADER_MAGIC
            }
        } catch (e: Exception) {
            false
        }
    }

    fun parseHeader(input: InputStream): SparseHeader? {
        val buf = ByteArray(28)
        val read = input.read(buf)
        if (read < 28) return null

        val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
        val magic = bb.int
        if (magic != SPARSE_HEADER_MAGIC) return null

        val major = bb.short
        val minor = bb.short
        val fileHdrSz = bb.short
        val chunkHdrSz = bb.short
        val blkSz = bb.int
        val totalBlks = bb.int
        val totalChks = bb.int
        val checksum = bb.int

        return SparseHeader(
            magic = magic,
            majorVersion = major,
            minorVersion = minor,
            fileHeaderSize = fileHdrSz,
            chunkHeaderSize = chunkHdrSz,
            blockSize = blkSz,
            totalBlocks = totalBlks,
            totalChunks = totalChks,
            imageChecksum = checksum
        )
    }

    fun getChunks(file: File, header: SparseHeader): List<SparseChunk> {
        val chunks = mutableListOf<SparseChunk>()
        FileInputStream(file).use { fis ->
            // Skip the file header
            val skipped = fis.skip(header.fileHeaderSize.toLong())
            var currentOffset = skipped

            val chunkHdrBuf = ByteArray(header.chunkHeaderSize.toInt())
            for (i in 0 until header.totalChunks) {
                val r = fis.read(chunkHdrBuf)
                if (r < header.chunkHeaderSize.toInt()) break
                currentOffset += r

                val bb = ByteBuffer.wrap(chunkHdrBuf).order(ByteOrder.LITTLE_ENDIAN)
                val chunkType = bb.short
                val reserved = bb.short
                val chunkBlocks = bb.int
                val totalSz = bb.int // size of chunk header + data in sparse file

                val dataSizeInFile = totalSz - header.chunkHeaderSize.toInt()
                val unsparseBytes = chunkBlocks.toLong() * header.blockSize

                when (chunkType) {
                    CHUNK_TYPE_RAW -> {
                        chunks.add(SparseChunk.Raw(chunkBlocks, unsparseBytes, currentOffset))
                        fis.skip(dataSizeInFile.toLong())
                        currentOffset += dataSizeInFile
                    }
                    CHUNK_TYPE_FILL -> {
                        val fillBuf = ByteArray(4)
                        fis.read(fillBuf)
                        currentOffset += 4
                        // skip rest if any
                        if (dataSizeInFile > 4) {
                            fis.skip((dataSizeInFile - 4).toLong())
                            currentOffset += (dataSizeInFile - 4)
                        }
                        chunks.add(SparseChunk.Fill(chunkBlocks, unsparseBytes, fillBuf))
                    }
                    CHUNK_TYPE_DONT_CARE -> {
                        chunks.add(SparseChunk.DontCare(chunkBlocks, unsparseBytes))
                        if (dataSizeInFile > 0) {
                            fis.skip(dataSizeInFile.toLong())
                            currentOffset += dataSizeInFile
                        }
                    }
                    CHUNK_TYPE_CRC32 -> {
                        val crcBuf = ByteArray(4)
                        fis.read(crcBuf)
                        currentOffset += 4
                        val crc = ByteBuffer.wrap(crcBuf).order(ByteOrder.LITTLE_ENDIAN).int
                        chunks.add(SparseChunk.Crc32(crc))
                    }
                    else -> {
                        // Unknown chunk, skip
                        if (dataSizeInFile > 0) {
                            fis.skip(dataSizeInFile.toLong())
                            currentOffset += dataSizeInFile
                        }
                    }
                }
            }
        }
        return chunks
    }
}
