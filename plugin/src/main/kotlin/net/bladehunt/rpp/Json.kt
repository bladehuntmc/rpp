package net.bladehunt.rpp

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
internal val Json = Json {
    allowComments = true
    ignoreUnknownKeys = true
    prettyPrint = false
}