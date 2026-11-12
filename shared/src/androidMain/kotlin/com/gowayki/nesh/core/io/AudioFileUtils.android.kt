package com.gowayki.nesh.core.io

import java.io.File

actual object AudioFileUtils {
    actual fun readBytes(filePath: String): ByteArray? {
        return runCatching {
            val file = File(filePath)
            if (file.exists() && file.isFile) file.readBytes() else null
        }.getOrNull()
    }
}

