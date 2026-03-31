package com.pocketai.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketai.app.data.preferences.PreferencesManager
import com.pocketai.app.data.repository.ExpenseRepository
import com.pocketai.app.services.ExportService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val exportService: ExportService,
    private val updateManager: com.pocketai.app.services.UpdateManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()
    
    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()
    
    private val _updateInfo = MutableStateFlow<com.pocketai.app.services.OtaManifest?>(null)
    val updateInfo: StateFlow<com.pocketai.app.services.OtaManifest?> = _updateInfo.asStateFlow()
    
    val appVersion: String = updateManager.getAppVersion()
    
    // V2.0: Profile Settings
    val userName = preferencesManager.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "User")

    val monthlyBudget = preferencesManager.monthlyBudget
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000f)

    fun updateUserName(name: String) {
        viewModelScope.launch {
            preferencesManager.setUserName(name)
        }
    }

    fun updateMonthlyBudget(budget: Float) {
        viewModelScope.launch {
            preferencesManager.setMonthlyBudget(budget)
        }
    }

    val themeMode = preferencesManager.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "auto")

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }

    val useGpu = preferencesManager.useGpu
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setUseGpu(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setUseGpu(enabled)
        }
    }

    fun exportData(onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val expenses = expenseRepository.getAllExpenses().first()
                val result = exportService.exportToCsv(expenses)
                onResult(result)
            } catch (e: Exception) {
                onResult(Result.failure(e))
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun clearAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            expenseRepository.deleteAllExpenses()
            onComplete()
        }
    }
    
    fun checkForUpdates(onNoUpdate: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            try {
                val update = updateManager.checkForUpdate()
                if (update != null) {
                    _updateInfo.value = update
                } else {
                    onNoUpdate()
                }
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            } finally {
                _isCheckingUpdate.value = false
            }
        }
    }
    
    fun startUpdate(url: String, version: String) {
        viewModelScope.launch {
            updateManager.downloadAndInstall(url, "PocketAI_v$version.apk")
        }
    }
    
    fun dismissUpdateDialog() {
        _updateInfo.value = null
    }
}
