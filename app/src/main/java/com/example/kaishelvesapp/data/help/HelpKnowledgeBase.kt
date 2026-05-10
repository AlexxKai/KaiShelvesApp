package com.example.kaishelvesapp.data.help

object HelpKnowledgeBase {
    val faq = listOf(
        "Para buscar libros usa la barra superior por título, autor o ISBN. El icono de cámara permite escanear códigos ISBN.",
        "La sección Descubrir muestra resultados del catálogo y permite abrir el detalle de un libro.",
        "Desde el detalle de un libro puedes marcarlo como leído y enviarlo a tus listas.",
        "Mis libros y Listas agrupan los libros guardados por el usuario.",
        "Biblioteca del dispositivo permite gestionar libros o archivos locales disponibles en el teléfono.",
        "Estadísticas muestra progreso lector y resumen de lecturas.",
        "Perfil permite revisar datos del usuario, privacidad y conexiones.",
        "Amigos muestra actividad social, solicitudes y perfiles de otros lectores.",
        "Si una sección está bloqueada como invitado, crea una cuenta para sincronizar y activar funciones sociales."
    )

    val onboarding = listOf(
        "Empieza en Descubrir buscando un libro o escaneando su ISBN.",
        "Abre un resultado para revisar su ficha y guardarlo o marcarlo como leído.",
        "Usa Mis libros para organizar tus lecturas en listas.",
        "Revisa Estadísticas cuando quieras ver tu progreso.",
        "Configura privacidad y datos desde Perfil."
    )

    val flows = listOf(
        "Buscar libro: escribe en la barra superior, pulsa buscar y abre un resultado.",
        "Escanear ISBN: pulsa la cámara de la barra superior, concede permiso y enfoca el código.",
        "Guardar lectura: abre el detalle del libro y usa la acción de marcar como leído.",
        "Ver notificaciones: pulsa la campana superior cuando haya solicitudes pendientes.",
        "Cambiar ajustes: abre el menú lateral, entra en Perfil y después en privacidad."
    )

    val commonErrors = listOf(
        "Si no aparecen resultados, revisa la conexión o prueba con menos palabras.",
        "Si el escáner no abre, concede permiso de cámara en Android.",
        "Si una función social no está disponible, puede que estés usando modo invitado.",
        "Si el catálogo tarda, espera unos segundos y reintenta la búsqueda.",
        "Si una portada no carga, el libro puede no tener imagen disponible en el proveedor."
    )

    fun asPromptText(): String {
        return buildString {
            appendLine("FAQ:")
            faq.forEach { appendLine("- $it") }
            appendLine("Onboarding:")
            onboarding.forEach { appendLine("- $it") }
            appendLine("Flujos:")
            flows.forEach { appendLine("- $it") }
            appendLine("Errores frecuentes:")
            commonErrors.forEach { appendLine("- $it") }
        }
    }
}
