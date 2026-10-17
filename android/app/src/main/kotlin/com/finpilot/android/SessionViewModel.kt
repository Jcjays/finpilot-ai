package com.finpilot.android

import androidx.lifecycle.ViewModel
import com.finpilot.android.core.network.auth.AuthRepository
import com.finpilot.android.core.network.auth.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** Exposes the Supabase session so the root composable can switch between sign-in and the app. */
@HiltViewModel
class SessionViewModel @Inject constructor(authRepository: AuthRepository) : ViewModel() {
    val sessionState: StateFlow<SessionState> = authRepository.sessionState
}
