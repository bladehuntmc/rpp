package net.bladehunt.rpp.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
val Json = Json {
    allowComments = true
    ignoreUnknownKeys = true
    allowTrailingComma = true
    prettyPrint = false
}