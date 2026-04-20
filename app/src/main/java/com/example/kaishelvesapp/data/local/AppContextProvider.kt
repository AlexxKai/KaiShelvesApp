package com.example.kaishelvesapp.data.local

import android.content.Context

object AppContextProvider {
    private lateinit var appContext: Context

    fun initialize(context: Context) {
        if (!::appContext.isInitialized) {
            appContext = context.applicationContext
        }
    }

    fun requireContext(): Context {
        check(::appContext.isInitialized) {
            "AppContextProvider no se ha inicializado"
        }
        return appContext
    }
}
