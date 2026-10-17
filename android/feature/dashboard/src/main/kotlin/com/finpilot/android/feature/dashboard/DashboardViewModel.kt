package com.finpilot.android.feature.dashboard

import android.util.Log
import com.finpilot.android.core.network.api.FinPilotApi
import com.finpilot.android.core.network.auth.AuthRepository
import com.finpilot.android.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

@HiltViewModel
class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val api: FinPilotApi,
    monthLabelProvider: () -> String,
) : MviViewModel<DashboardState, DashboardIntent, DashboardEffect>(
    DashboardState(monthLabel = monthLabelProvider()),
) {

    @Inject
    constructor(authRepository: AuthRepository, api: FinPilotApi) :
        this(authRepository, api, ::currentManilaMonthLabel)

    init {
        dispatch(DashboardIntent.Refresh)
    }

    override suspend fun handle(intent: DashboardIntent) {
        when (intent) {
            DashboardIntent.Refresh -> loadProfile()
            DashboardIntent.SignOut -> authRepository.signOut()
        }
    }

    /** Milestone 2: the only backend data on the dashboard is the caller's profile, which proves the token flow. */
    private suspend fun loadProfile() {
        reduce { copy(isRefreshing = true) }
        try {
            val profile = api.me().data
            reduce { copy(isRefreshing = false, profileEmail = profile.email, currency = profile.currency) }
        } catch (error: IOException) {
            reportLoadFailure(error)
        } catch (error: HttpException) {
            reportLoadFailure(error)
        } catch (error: SerializationException) {
            reportLoadFailure(error)
        }
    }

    private suspend fun reportLoadFailure(error: Exception) {
        Log.w(TAG, "Could not load the profile", error)
        reduce { copy(isRefreshing = false) }
        emit(DashboardEffect.ShowMessage(PROFILE_ERROR_MESSAGE))
    }

    private companion object {
        const val TAG = "DashboardViewModel"
        const val PROFILE_ERROR_MESSAGE = "Could not load your profile. Tap Refresh to try again."
    }
}

private val manilaZone: ZoneId = ZoneId.of("Asia/Manila")
private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

private fun currentManilaMonthLabel(): String = YearMonth.now(manilaZone).format(monthFormatter)
