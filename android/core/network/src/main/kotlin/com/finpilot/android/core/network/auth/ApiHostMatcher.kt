package com.finpilot.android.core.network.auth

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/** Decides whether a URL belongs to the FinPilot API, so bearer tokens are never sent to other hosts. */
internal class ApiHostMatcher(apiBaseUrl: String) {
    private val base: HttpUrl = apiBaseUrl.toHttpUrl()

    fun matches(url: HttpUrl): Boolean =
        url.scheme == base.scheme && url.host.equals(base.host, ignoreCase = true) && url.port == base.port
}
