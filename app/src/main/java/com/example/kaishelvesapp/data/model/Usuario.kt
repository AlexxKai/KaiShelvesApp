package com.example.kaishelvesapp.data.model

data class UserPrivacySettings(
    val profileVisible: Boolean = true,
    val emailVisible: Boolean = false,
    val readingActivityVisible: Boolean = true,
    val friendsVisible: Boolean = true,
    val friendRequestPermissions: Boolean = true,
    val socialInteractionPermissions: Boolean = true,
    val personalizedSuggestions: Boolean = true
)

data class Usuario(
    val uid: String = "",
    val usuario: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val isAdmin: Boolean = false,
    val isGuest: Boolean = false,
    val privacySettings: UserPrivacySettings = UserPrivacySettings()
)
