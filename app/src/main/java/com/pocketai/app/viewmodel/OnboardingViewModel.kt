package com.pocketai.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketai.app.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    fun completeOnboarding(name: String, budget: Float) {
        viewModelScope.launch {
            preferencesManager.setUserName(name)
            preferencesManager.setMonthlyBudget(budget)
            preferencesManager.setOnboardingCompleted(true)
        }
    }
}
