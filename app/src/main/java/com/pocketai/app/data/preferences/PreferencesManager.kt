package com.pocketai.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Manages user preferences for v2.0 (Onboarding, Profile, Settings).
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    
    companion object {
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val MONTHLY_BUDGET = floatPreferencesKey("monthly_budget")
        private val THEME_MODE = stringPreferencesKey("theme_mode") // "light", "dark", "auto"
        private val LOGO_API_KEY = stringPreferencesKey("logo_api_key")
    }
    
    val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED] ?: false
    }
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }
    
    val userName: Flow<String> = dataStore.data.map { preferences ->
        preferences[USER_NAME] ?: "User"
    }
    
    suspend fun setUserName(name: String) {
        dataStore.edit { it[USER_NAME] = name }
    }
    
    val monthlyBudget: Flow<Float> = dataStore.data.map { preferences ->
        preferences[MONTHLY_BUDGET] ?: 2000f
    }
    
    suspend fun setMonthlyBudget(budget: Float) {
        dataStore.edit { it[MONTHLY_BUDGET] = budget }
    }
    
    val themeMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "auto"
    }
    
    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[THEME_MODE] = mode }
    }
    
    val logoApiKey: Flow<String> = dataStore.data.map { preferences ->
        preferences[LOGO_API_KEY] ?: ""
    }
    
    suspend fun setLogoApiKey(key: String) {
        dataStore.edit { it[LOGO_API_KEY] = key }
    }
}
