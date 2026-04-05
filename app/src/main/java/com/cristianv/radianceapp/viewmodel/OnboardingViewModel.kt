package com.cristianv.radianceapp.viewmodel

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val android.content.Context.onboardingDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "onboarding_prefs")

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.onboardingDataStore

    companion object {
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
    }

    private val _hasSeenOnboarding = MutableStateFlow<Boolean?>(null)
    val hasSeenOnboarding: StateFlow<Boolean?> = _hasSeenOnboarding

    init {
        viewModelScope.launch {
            val value = dataStore.data
                .map { prefs -> prefs[HAS_SEEN_ONBOARDING] ?: false }
                .first()
            _hasSeenOnboarding.value = value
        }
    }

    fun markOnboardingSeen() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[HAS_SEEN_ONBOARDING] = true
            }
            _hasSeenOnboarding.value = true
        }
    }
}
