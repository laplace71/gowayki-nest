package com.gowayki.nesh.core.io

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

actual object AudioFileUtils {
    @OptIn(ExperimentalForeignApi::class)
    actual fun readBytes(filePath: String): ByteArray? {
        val data = NSData.dataWithContentsOfFile(filePath) ?: return null
        val length = data.length.toInt()
        val bytes = ByteArray(length)
        if (length > 0) {
            bytes.usePinned { pinned ->
                memcpy(pinned.addressOf(0), data.bytes, data.length)
            }
        }
        return bytes
    }
}

