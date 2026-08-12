//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition.internal

import java.io.EOFException
import java.io.File
import java.io.InputStream

/**
 * Reads the tar (ustar) stream of a study bundle archive.
 *
 * Supports the subset the Swift exporter writes: plain files and directories with paths that fit
 * the 100-character name field. Other entry kinds (links, pax metadata) are skipped.
 */
internal object TarReader {
    private const val BLOCK_SIZE = 512
    private const val COPY_BUFFER_SIZE = BLOCK_SIZE * 16

    // Far beyond any real study bundle, but a bound on what a hostile archive can write.
    private const val MAX_EXTRACTED_BYTES = 1L shl 30
    private const val NAME_OFFSET = 0
    private const val NAME_LENGTH = 100
    private const val SIZE_OFFSET = 124
    private const val SIZE_LENGTH = 12
    private const val TYPE_OFFSET = 156
    private const val TYPE_DIRECTORY = '5'.code.toByte()
    private const val TYPE_FILE = '0'.code.toByte()
    private const val TYPE_FILE_LEGACY = 0.toByte()

    /**
     * Extracts every entry of the tar stream read from [input] into [into].
     *
     * @throws IllegalArgumentException when an entry path would escape [into].
     * @throws EOFException when the stream ends before the archive does.
     */
    fun extract(input: InputStream, into: File) {
        into.mkdirs()
        val root = into.canonicalFile
        val header = ByteArray(BLOCK_SIZE)
        var extractedBytes = 0L
        while (true) {
            if (!readBlock(input, header)) throw EOFException("Missing tar end-of-archive marker")
            if (header.all { it == 0.toByte() }) {
                // The end-of-archive marker is two consecutive zero blocks.
                if (!readBlock(input, header) || header.any { it != 0.toByte() }) {
                    throw EOFException("Malformed tar end-of-archive marker")
                }
                return
            }
            val name = string(header, NAME_OFFSET, NAME_LENGTH)
            val size = string(header, SIZE_OFFSET, SIZE_LENGTH).trim().toLong(radix = 8)
            require(size >= 0) { "Malformed archive entry size: $size" }
            extractedBytes += size
            require(extractedBytes <= MAX_EXTRACTED_BYTES) { "Archive exceeds the permitted size" }
            val target = File(root, name).canonicalFile
            require(target.path == root.path || target.path.startsWith(root.path + File.separator)) {
                "Archive entry escapes the bundle: $name"
            }
            when (header[TYPE_OFFSET]) {
                TYPE_DIRECTORY -> target.mkdirs()
                TYPE_FILE, TYPE_FILE_LEGACY -> {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { output -> copy(input, output, size) }
                }
                else -> skip(input, size)
            }
            skip(input, padding(size))
        }
    }

    private fun readBlock(input: InputStream, block: ByteArray): Boolean {
        var read = 0
        while (read < block.size) {
            val count = input.read(block, read, block.size - read)
            if (count < 0) {
                if (read == 0) return false
                throw EOFException("Truncated tar header")
            }
            read += count
        }
        return true
    }

    private fun copy(input: InputStream, output: java.io.OutputStream, size: Long) {
        val buffer = ByteArray(COPY_BUFFER_SIZE)
        var remaining = size
        while (remaining > 0) {
            val count = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (count < 0) throw EOFException("Truncated tar entry")
            output.write(buffer, 0, count)
            remaining -= count
        }
    }

    private fun skip(input: InputStream, count: Long) {
        var remaining = count
        val buffer = ByteArray(BLOCK_SIZE)
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read < 0) throw EOFException("Truncated tar stream")
            remaining -= read
        }
    }

    private fun padding(size: Long): Long {
        val remainder = size % BLOCK_SIZE
        return if (remainder == 0L) 0 else BLOCK_SIZE - remainder
    }

    private fun string(header: ByteArray, offset: Int, length: Int): String {
        val field = header.copyOfRange(offset, offset + length)
        val end = field.indexOfFirst { it == 0.toByte() }.let { if (it < 0) field.size else it }
        return String(field, 0, end, Charsets.UTF_8)
    }
}
