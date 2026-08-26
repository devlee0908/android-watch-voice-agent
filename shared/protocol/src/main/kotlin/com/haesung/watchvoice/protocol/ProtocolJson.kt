package com.haesung.watchvoice.protocol

import kotlinx.serialization.json.Json

/**
 * The single JSON configuration used on both sides. [Json.ignoreUnknownKeys] keeps an older peer
 * able to read messages from a newer one within the same protocol version.
 */
val ProtocolJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun CommandEnvelope.encode(): ByteArray =
    ProtocolJson.encodeToString(CommandEnvelope.serializer(), this).encodeToByteArray()

fun decodeEnvelope(bytes: ByteArray): CommandEnvelope =
    ProtocolJson.decodeFromString(CommandEnvelope.serializer(), bytes.decodeToString())

fun CommandResult.encode(): ByteArray =
    ProtocolJson.encodeToString(CommandResult.serializer(), this).encodeToByteArray()

fun decodeResult(bytes: ByteArray): CommandResult =
    ProtocolJson.decodeFromString(CommandResult.serializer(), bytes.decodeToString())
