package com.example.kaishelvesapp.data.help

object HelpKnowledgeBase {
    val faq = listOf(
        "Para buscar libros usa la barra superior por titulo, autor o ISBN. El icono de camara permite escanear codigos ISBN.",
        "La seccion Descubrir muestra resultados del catalogo y permite abrir el detalle de un libro.",
        "Desde el detalle de un libro puedes marcarlo como leido y enviarlo a tus listas.",
        "Mis libros y Listas agrupan los libros guardados por el usuario.",
        "Biblioteca del dispositivo permite gestionar libros o archivos locales disponibles en el telefono.",
        "Estadisticas muestra progreso lector y resumen de lecturas.",
        "Perfil permite revisar datos del usuario, privacidad y conexiones.",
        "Amigos muestra actividad social, solicitudes y perfiles de otros lectores.",
        "Si una seccion esta bloqueada como invitado, crea una cuenta para sincronizar y activar funciones sociales."
    )

    val onboarding = listOf(
        "Empieza en Descubrir buscando un libro o escaneando su ISBN.",
        "Abre un resultado para revisar su ficha y guardarlo o marcarlo como leido.",
        "Usa Mis libros para organizar tus lecturas en listas.",
        "Revisa Estadisticas cuando quieras ver tu progreso.",
        "Configura privacidad y datos desde Perfil."
    )

    val flows = listOf(
        "Buscar libro: escribe en la barra superior, pulsa buscar y abre un resultado.",
        "Escanear ISBN: pulsa la camara de la barra superior, concede permiso y enfoca el codigo.",
        "Guardar lectura: abre el detalle del libro y usa la accion de marcar como leido.",
        "Ver notificaciones: pulsa la campana superior cuando haya solicitudes pendientes.",
        "Cambiar ajustes: abre el menu lateral, entra en Perfil y despues en privacidad."
    )

    val commonErrors = listOf(
        "Si no aparecen resultados, revisa la conexion o prueba con menos palabras.",
        "Si el escaner no abre, concede permiso de camara en Android.",
        "Si una funcion social no esta disponible, puede que estes usando modo invitado.",
        "Si el catalogo tarda, espera unos segundos y reintenta la busqueda.",
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
