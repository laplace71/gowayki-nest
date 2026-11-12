package com.gowayki.nesh.core.io

expect object AudioFileUtils {
    fun readBytes(filePath: String): ByteArray?
}

