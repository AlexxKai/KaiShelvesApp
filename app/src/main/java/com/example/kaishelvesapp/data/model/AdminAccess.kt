package com.example.kaishelvesapp.data.model

object AdminAccess {
    private const val BootstrapAdminEmail = "anbemaur@hotmail.com"

    @Suppress("UNUSED_PARAMETER")
    fun isBootstrapAdminAccount(uid: String, email: String): Boolean {
        return email.equals(BootstrapAdminEmail, ignoreCase = true)
    }
}
