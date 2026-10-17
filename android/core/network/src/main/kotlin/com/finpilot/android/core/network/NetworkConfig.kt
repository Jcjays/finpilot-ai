package com.finpilot.android.core.network

/** Endpoints and the publishable Supabase key. Provided by the app module from BuildConfig. */
data class NetworkConfig(
    val apiBaseUrl: String,
    val supabaseUrl: String,
    val supabasePublishableKey: String,
)
