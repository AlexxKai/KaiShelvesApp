package com.example.kaishelvesapp.data.remote.inventaire

import com.example.kaishelvesapp.data.model.Libro
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

private val preferredLabelLanguages = listOf("es", "nl", "en", "fr", "de")

private fun JsonObject.objectOrNull(name: String): JsonObject? {
    return get(name)?.takeIf { it.isJsonObject }?.asJsonObject
}

private fun JsonObject.arrayOrNull(name: String): JsonArray? {
    return get(name)?.takeIf { it.isJsonArray }?.asJsonArray
}

private fun JsonElement.stringValue(): String? {
    return when {
        isJsonPrimitive && asJsonPrimitive.isString -> asString
        isJsonPrimitive && asJsonPrimitive.isNumber -> asString
        isJsonObject -> asJsonObject.get("value")?.stringValue()
            ?: asJsonObject.get("text")?.stringValue()
            ?: asJsonObject.get("id")?.stringValue()
        else -> null
    }?.trim()?.takeIf { it.isNotBlank() }
}

private fun JsonObject.bestLabel(): String {
    val labels = objectOrNull("labels") ?: return ""
    preferredLabelLanguages.forEach { language ->
        labels.get(language)?.stringValue()?.let { return it }
    }
    return labels.entrySet()
        .firstOrNull()
        ?.value
        ?.stringValue()
        .orEmpty()
}

private fun JsonObject.claimValues(property: String): List<JsonElement> {
    val claims = objectOrNull("claims") ?: return emptyList()
    val claimValue = claims.get(property) ?: return emptyList()
    return when {
        claimValue.isJsonArray -> claimValue.asJsonArray.toList()
        else -> listOf(claimValue)
    }
}

private fun JsonObject.claimValues(vararg properties: String): List<JsonElement> {
    return properties.flatMap { property -> claimValues(property) }
}

private fun JsonObject.firstClaimString(vararg properties: String): String {
    properties.forEach { property ->
        claimValues(property)
            .mapNotNull { it.stringValue() }
            .firstOrNull()
            ?.let { return it }
    }
    return ""
}

private fun JsonObject.firstClaimInt(vararg properties: String): Int {
    return firstClaimString(*properties).toIntOrNull() ?: 0
}

private fun parsePublishedYear(value: String): Int {
    return Regex("\\d{4}")
        .find(value)
        ?.value
        ?.toIntOrNull()
        ?: 0
}

private fun entitiesObject(response: JsonObject): JsonObject {
    return response.objectOrNull("entities") ?: JsonObject()
}

private fun entityById(entities: JsonObject, id: String): JsonObject? {
    return entities.get(id)?.takeIf { it.isJsonObject }?.asJsonObject
}

private fun labelsForEntityIds(
    entities: JsonObject,
    ids: List<String>
): String {
    return ids
        .mapNotNull { id -> entityById(entities, id)?.bestLabel()?.takeIf(String::isNotBlank) }
        .distinct()
        .joinToString(", ")
}

private fun authorFromEditionOrWork(
    edition: JsonObject,
    entities: JsonObject
): String {
    val directAuthorIds = edition.claimValues("wdt:P50")
        .mapNotNull { it.stringValue() }
    labelsForEntityIds(entities, directAuthorIds)
        .takeIf(String::isNotBlank)
        ?.let { return it }

    val workIds = edition.claimValues("wdt:P629")
        .mapNotNull { it.stringValue() }
    val workAuthorIds = workIds
        .mapNotNull { entityById(entities, it) }
        .flatMap { work -> work.claimValues("wdt:P50").mapNotNull { it.stringValue() } }
    return labelsForEntityIds(entities, workAuthorIds)
}

fun inventaireBookFromResponse(
    response: JsonObject,
    isbn: String
): Libro? {
    val entities = entitiesObject(response)
    val edition = entities.get("isbn:$isbn")?.takeIf { it.isJsonObject }?.asJsonObject
        ?: entities.entrySet()
            .firstOrNull { (_, value) ->
                value.isJsonObject && value.asJsonObject.claimValues("wdt:P212")
                    .mapNotNull { it.stringValue() }
                    .contains(isbn)
            }
            ?.value
            ?.asJsonObject
        ?: return null

    val title = edition.bestLabel()
        .ifBlank { edition.firstClaimString("wdt:P1476") }
        .ifBlank {
            edition.claimValues("wdt:P629")
                .mapNotNull { it.stringValue() }
                .mapNotNull { entityById(entities, it)?.bestLabel() }
                .firstOrNull()
                .orEmpty()
        }

    val author = authorFromEditionOrWork(edition, entities)
    if (title.isBlank() && author.isBlank()) return null

    val publisher = labelsForEntityIds(
        entities = entities,
        ids = edition.claimValues("wdt:P123").mapNotNull { it.stringValue() }
    )
    val genre = labelsForEntityIds(
        entities = entities,
        ids = edition.claimValues("wdt:P136", "wdt:P921").mapNotNull { it.stringValue() }
    )

    return Libro(
        id = isbn,
        isbn = isbn,
        titulo = title,
        autor = author,
        editorial = publisher,
        genero = genre,
        fechaPublicacion = parsePublishedYear(edition.firstClaimString("wdt:P577")),
        paginas = edition.firstClaimInt("wdt:P1104"),
        averageRating = 0.0,
        ratingsCount = 0,
        imagen = "",
        pdf = "https://inventaire.io/entity/isbn:$isbn"
    )
}
