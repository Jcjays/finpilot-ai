package com.finpilot.android

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finpilot.android.feature.auth.SignInTestTags
import com.finpilot.android.feature.dashboard.DashboardTestTags
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end: sign up through Supabase Auth, land on the dashboard, and see the profile that the backend
 * created from the bearer token. Requires the local Supabase stack and the backend to be running
 * (see docs/supabase-setup.md); the test is skipped, not failed, when the backend is unreachable.
 * Run with a signed-out app (`adb shell pm clear com.finpilot.android`).
 */
@RunWith(AndroidJUnit4::class)
class SignUpToDashboardE2ETest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun requireLocalBackend() {
        assumeTrue("backend not reachable at ${BuildConfig.API_BASE_URL}", backendReachable())
    }

    @Test
    fun signUpCreatesTheProfileAndShowsItOnTheDashboard() {
        val email = "e2e${System.currentTimeMillis()}@example.test"

        compose.waitUntil(STARTUP_TIMEOUT_MS) {
            compose.onAllNodesWithTag(SignInTestTags.TOGGLE_MODE).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(SignInTestTags.TOGGLE_MODE).performClick()
        compose.onNodeWithTag(SignInTestTags.EMAIL).performTextInput(email)
        compose.onNodeWithTag(SignInTestTags.PASSWORD).performTextInput("longenough1")
        compose.onNodeWithTag(SignInTestTags.SUBMIT).performClick()

        compose.waitUntil(NETWORK_TIMEOUT_MS) {
            compose.onAllNodesWithTag(DashboardTestTags.PROFILE).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitUntil(NETWORK_TIMEOUT_MS) {
            runCatching {
                compose.onNodeWithTag(DashboardTestTags.PROFILE).assertTextContains(email, substring = true)
            }.isSuccess
        }
        compose.onNodeWithTag(DashboardTestTags.PROFILE).assertTextContains("PHP", substring = true)
    }

    private fun backendReachable(): Boolean {
        val client = OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS).build()
        val request = Request.Builder().url(BuildConfig.API_BASE_URL + "health").build()
        return runCatching { client.newCall(request).execute().use { it.isSuccessful } }.getOrDefault(false)
    }

    private companion object {
        const val STARTUP_TIMEOUT_MS = 10_000L
        const val NETWORK_TIMEOUT_MS = 30_000L
    }
}
