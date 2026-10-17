package com.finpilot.android.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finpilot.android.core.ui.theme.FinPilotTheme

/** Navigation entry: owns the ViewModel, collects state and one-shot effects. */
@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DashboardEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text)
            }
        }
    }

    DashboardScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::dispatch,
    )
}

/** Stateless, previewable screen. Milestone 1 placeholder until finance data arrives in Milestone 3. */
@Composable
fun DashboardScreen(
    state: DashboardState,
    onIntent: (DashboardIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "FinPilot AI",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.monthLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = state.profileEmail?.let { "Signed in as $it · ${state.currency.orEmpty()}" }
                    ?: "Loading your profile…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(DashboardTestTags.PROFILE),
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onIntent(DashboardIntent.Refresh) },
                enabled = !state.isRefreshing,
            ) {
                Text(if (state.isRefreshing) "Refreshing…" else "Refresh")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { onIntent(DashboardIntent.SignOut) }) {
                Text("Sign out")
            }
        }
    }
}

@Preview(showBackground = true, name = "Dashboard – idle")
@Composable
private fun DashboardScreenPreview() {
    FinPilotTheme {
        DashboardScreen(
            state = DashboardState(monthLabel = "October 2026", profileEmail = "chris@example.test", currency = "PHP"),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, name = "Dashboard – refreshing", uiMode = 0x20)
@Composable
private fun DashboardScreenRefreshingDarkPreview() {
    FinPilotTheme(darkTheme = true) {
        DashboardScreen(
            state = DashboardState(monthLabel = "October 2026", isRefreshing = true),
            onIntent = {},
        )
    }
}
