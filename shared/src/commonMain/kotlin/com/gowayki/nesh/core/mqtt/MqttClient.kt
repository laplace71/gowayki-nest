package com.gowayki.nesh.core.mqtt

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readByte
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Cliente MQTT 3.1.1 mínimo sobre TCP (Ktor sockets), multiplataforma.
 */
object MqttClient {
    private const val HOST = "31.220.72.2"
    private const val PORT = 1883
    private const val USERNAME = "mqttuser"
    private const val PASSWORD = "tecsup123"
    private const val CLIENT_ID = "wayki-nest-android"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @kotlin.concurrent.Volatile
    private var connected = false

    private var socket: io.ktor.network.sockets.Socket? = null
    private var input: io.ktor.utils.io.ByteReadChannel? = null
    private var output: io.ktor.utils.io.ByteWriteChannel? = null

    fun connectAsync() {
        scope.launch { runCatching { connect() } }
    }

    private suspend fun connect() {
        if (connected) return
        try {
            val selector = SelectorManager(Dispatchers.Default)
            val s = aSocket(selector).tcp().connect(HOST, PORT)
            socket = s
            input = s.openReadChannel()
            output = s.openWriteChannel(autoFlush = true)
            val packet = buildConnect(CLIENT_ID, USERNAME, PASSWORD)
            output!!.writeFully(packet)
            output!!.flush()
            input!!.readByte()
            input!!.readByte()
            input!!.readByte()
            val b3 = input!!.readByte()
            if ((b3.toInt() and 0xFF) != 0) {
                throw IllegalStateException("CONNACK return code ${b3.toInt() and 0xFF}")
            }
            connected = true
        } catch (t: Throwable) {
            connected = false
            try { socket?.close() } catch (_: Throwable) {}
            socket = null
            input = null
            output = null
        }
    }

    fun publish(topic: String, message: String) {
        scope.launch {
            if (!connected) connect()
            if (!connected) return@launch
            try {
                val packet = buildPublish(topic, message)
                output!!.writeFully(packet)
                output!!.flush()
            } catch (t: Throwable) {
                connected = false
                try { socket?.close() } catch (_: Throwable) {}
                socket = null
                input = null
                output = null
            }
        }
    }

    private fun buildConnect(clientId: String, username: String?, password: String?): ByteArray {
        val vh = mutableListOf<Byte>()
        writeMqttString(vh, "MQTT")
        vh.add(0x04)
        var flags = 0x02
        if (username != null) flags = flags or 0x80
        if (password != null) flags = flags or 0x40
        vh.add(flags.toByte())
        vh.add(0x00)
        vh.add(60.toByte())
        val payload = mutableListOf<Byte>()
        writeMqttString(payload, clientId)
        if (username != null) writeMqttString(payload, username)
        if (password != null) writeMqttString(payload, password)

        val remaining = vh.size + payload.size
        val out = mutableListOf<Byte>()
        out.add(0x10)
        writeRemainingLength(out, remaining)
        out.addAll(vh)
        out.addAll(payload)
        return toByteArray(out)
    }

    private fun buildPublish(topic: String, message: String): ByteArray {
        val topicBytes = topic.encodeToByteArray()
        val msgBytes = message.encodeToByteArray()
        val vh = mutableListOf<Byte>()
        vh.add(((topicBytes.size shr 8) and 0xFF).toByte())
        vh.add((topicBytes.size and 0xFF).toByte())
        vh.addAll(topicBytes.toList())
        val remaining = vh.size + msgBytes.size
        val out = mutableListOf<Byte>()
        out.add(0x30)
        writeRemainingLength(out, remaining)
        out.addAll(vh)
        out.addAll(msgBytes.toList())
        return toByteArray(out)
    }

    private fun writeMqttString(out: MutableList<Byte>, value: String) {
        val b = value.encodeToByteArray()
        out.add(((b.size shr 8) and 0xFF).toByte())
        out.add((b.size and 0xFF).toByte())
        out.addAll(b.toList())
    }

    private fun writeRemainingLength(out: MutableList<Byte>, length: Int) {
        var x = length
        do {
            var b = x % 128
            x /= 128
            if (x > 0) b = b or 0x80
            out.add(b.toByte())
        } while (x > 0)
    }

    private fun toByteArray(list: List<Byte>): ByteArray {
        val arr = ByteArray(list.size)
        for (i in list.indices) arr[i] = list[i]
        return arr
    }
}

object MqttTopics {
    const val RECORDING = "wayki/grabacion"
    const val MAP_ALERT = "wayki/alerta/mapa"
    const val MSG_RECORDING = "se creó una grabación"
    const val MSG_MAP_ALERT = "se creó una alerta de mapa"
}
