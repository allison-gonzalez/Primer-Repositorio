package com.example.mobileapp.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.common.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    private object Keys {
        val CONSENT_GRANTED = booleanPreferencesKey("consent_granted")
        val DAILY_STEP_GOAL = intPreferencesKey("daily_step_goal")
    }

    // RNF-09: la app no debe recolectar datos hasta que el usuario acepte el consentimiento.
    val consentGranted: Flow<Boolean> =
        context.userPrefsDataStore.data.map { it[Keys.CONSENT_GRANTED] ?: false }

    // RF-14: meta diaria de pasos configurable por el usuario.
    val dailyStepGoal: Flow<Int> =
        context.userPrefsDataStore.data.map { it[Keys.DAILY_STEP_GOAL] ?: Constants.DEFAULT_DAILY_STEP_GOAL }

    suspend fun setConsentGranted(granted: Boolean) {
        context.userPrefsDataStore.edit { it[Keys.CONSENT_GRANTED] = granted }
    }

    suspend fun setDailyStepGoal(goal: Int) {
        context.userPrefsDataStore.edit { it[Keys.DAILY_STEP_GOAL] = goal }
    }
}
