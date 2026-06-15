package com.example.kaishelvesapp.data.help

import com.example.kaishelvesapp.ui.language.LanguageManager

object HelpKnowledgeBase {
    val faq: List<String>
        get() = localized(spanishFaq, englishFaq)

    val onboarding: List<String>
        get() = localized(spanishOnboarding, englishOnboarding)

    val flows: List<String>
        get() = localized(spanishFlows, englishFlows)

    val commonErrors: List<String>
        get() = localized(spanishCommonErrors, englishCommonErrors)

    val communityRules: List<String>
        get() = localized(spanishCommunityRules, englishCommunityRules)

    fun asPromptText(languageTag: String = LanguageManager.getCurrentLanguage()): String {
        val spanish = languageTag == "es"
        return buildString {
            appendLine(if (spanish) "Preguntas frecuentes:" else "Frequently asked questions:")
            localized(spanishFaq, englishFaq, languageTag).forEach { appendLine("- $it") }
            appendLine(if (spanish) "Primeros pasos:" else "Getting started:")
            localized(spanishOnboarding, englishOnboarding, languageTag).forEach { appendLine("- $it") }
            appendLine(if (spanish) "Flujos guiados:" else "Guided flows:")
            localized(spanishFlows, englishFlows, languageTag).forEach { appendLine("- $it") }
            appendLine(if (spanish) "Problemas frecuentes:" else "Common issues:")
            localized(spanishCommonErrors, englishCommonErrors, languageTag).forEach { appendLine("- $it") }
        }
    }

    private fun localized(spanish: List<String>, english: List<String>): List<String> {
        return localized(spanish, english, LanguageManager.getCurrentLanguage())
    }

    private fun localized(spanish: List<String>, english: List<String>, languageTag: String): List<String> {
        return if (languageTag == "es") spanish else english
    }

    private val spanishFaq = listOf(
        "Para buscar libros, usa la barra superior e introduce un título, un autor o un ISBN. El icono de la cámara permite escanear códigos ISBN.",
        "La sección Descubrir muestra resultados del catálogo y permite abrir los detalles de cada libro.",
        "Desde los detalles de un libro puedes marcarlo como leído y añadirlo a tus listas.",
        "Mis libros agrupa las listas y etiquetas que utilizas para organizar tus lecturas.",
        "La biblioteca del dispositivo permite gestionar los libros y archivos locales disponibles en el teléfono.",
        "Estadísticas muestra tu progreso lector y un resumen de tus lecturas.",
        "Perfil permite revisar tus datos, preferencias de privacidad y conexiones.",
        "Amigos muestra actividad social, solicitudes y perfiles de otros lectores.",
        "Si una sección está bloqueada en modo invitado, crea una cuenta para sincronizar tus datos y activar las funciones sociales."
    )

    private val englishFaq = listOf(
        "To search for books, use the top bar and enter a title, author, or ISBN. The camera icon lets you scan ISBN codes.",
        "Discover shows catalog results and lets you open each book's details.",
        "From a book's details, you can mark it as read and add it to your lists.",
        "My books groups the lists and tags you use to organize your reading.",
        "The device library lets you manage books and local files available on your phone.",
        "Statistics shows your reading progress and a summary of your reading.",
        "Profile lets you review your details, privacy preferences, and connections.",
        "Friends shows social activity, requests, and other readers' profiles.",
        "If a section is unavailable in guest mode, create an account to sync your data and enable social features."
    )

    private val spanishOnboarding = listOf(
        "Empieza en Descubrir buscando un libro o escaneando su ISBN.",
        "Abre un resultado para revisar su ficha, guardarlo o marcarlo como leído.",
        "Usa Mis libros para organizar tus lecturas en listas y etiquetas.",
        "Consulta Estadísticas cuando quieras revisar tu progreso.",
        "Configura la privacidad y tus datos desde Perfil."
    )

    private val englishOnboarding = listOf(
        "Start in Discover by searching for a book or scanning its ISBN.",
        "Open a result to review its details, save it, or mark it as read.",
        "Use My books to organize your reading with lists and tags.",
        "Open Statistics whenever you want to review your progress.",
        "Configure privacy and your personal data from Profile."
    )

    private val spanishFlows = listOf(
        "Buscar un libro: escribe en la barra superior, pulsa buscar y abre un resultado.",
        "Escanear un ISBN: pulsa la cámara de la barra superior, concede el permiso y enfoca el código.",
        "Guardar una lectura: abre los detalles del libro y usa la acción para marcarlo como leído.",
        "Ver notificaciones: pulsa la campana superior cuando haya solicitudes pendientes.",
        "Cambiar ajustes: abre el menú lateral, entra en Perfil y selecciona Ajustes."
    )

    private val englishFlows = listOf(
        "Search for a book: type in the top bar, start the search, and open a result.",
        "Scan an ISBN: tap the camera in the top bar, grant permission, and point it at the code.",
        "Save a reading: open the book's details and use the action to mark it as read.",
        "View notifications: tap the bell in the top bar when you have pending requests.",
        "Change settings: open the side menu, go to Profile, and select Settings."
    )

    private val spanishCommonErrors = listOf(
        "Si no aparecen resultados, revisa la conexión o prueba con menos palabras.",
        "Si el escáner no se abre, concede el permiso de cámara desde los ajustes de Android.",
        "Si una función social no está disponible, puede que estés usando el modo invitado.",
        "Si el catálogo tarda en responder, espera unos segundos e inténtalo de nuevo.",
        "Si una portada no se carga, es posible que el proveedor no disponga de una imagen para ese libro."
    )

    private val englishCommonErrors = listOf(
        "If no results appear, check your connection or try using fewer words.",
        "If the scanner does not open, grant camera permission from Android settings.",
        "If a social feature is unavailable, you may be using guest mode.",
        "If the catalog is slow to respond, wait a few seconds and try again.",
        "If a cover does not load, the provider may not have an image for that book."
    )

    private val spanishCommunityRules = listOf(
        "Trata a otros usuarios con respeto: no se permiten insultos, acoso, amenazas ni provocaciones.",
        "Evita el spam, la publicidad repetitiva y los mensajes que no aporten a la conversación.",
        "El nombre de usuario debe ser legible y respetuoso; no puede suplantar a otras personas ni usar términos reservados como administrador.",
        "La imagen de perfil debe ser adecuada para una comunidad lectora y no incluir contenido ofensivo, explícito o violento.",
        "Respeta la privacidad: no publiques datos personales de otros usuarios ni conversaciones privadas sin permiso."
    )

    private val englishCommunityRules = listOf(
        "Treat other users with respect: insults, harassment, threats and provocation are not allowed.",
        "Avoid spam, repetitive advertising and messages that do not contribute to the conversation.",
        "Your username must be readable and respectful; it cannot impersonate others or use reserved terms such as administrator.",
        "Your profile image must be appropriate for a reading community and must not include offensive, explicit or violent content.",
        "Respect privacy: do not publish other users' personal data or private conversations without permission."
    )
}
