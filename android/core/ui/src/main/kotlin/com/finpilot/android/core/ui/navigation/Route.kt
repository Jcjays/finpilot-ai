package com.finpilot.android.core.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Navigation 3 back-stack keys. Every screen is a serializable key so the stack survives process death. */
@Serializable
sealed interface Route : NavKey {

    @Serializable
    data object Dashboard : Route
}
