package com.antigravity.adaptivehz.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.IOException
import com.antigravity.adaptivehz.core.LanguageHelper

enum class OperationMode {
    ADAPTIVE,
    FIXED_60,
    FIXED_120,
    ECO
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = SettingsRepository.PREFS_NAME,
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, SettingsRepository.PREFS_NAME))
    }
)

class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dataStore: DataStore<Preferences> = appContext.dataStore
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _operationMode = MutableStateFlow(OperationMode.ADAPTIVE)
    val operationMode: StateFlow<OperationMode> = _operationMode.asStateFlow()

    private val _isAdaptiveEnabled = MutableStateFlow(true)
    val isAdaptiveEnabled: StateFlow<Boolean> = _isAdaptiveEnabled.asStateFlow()

    private val _idleTimeoutSeconds = MutableStateFlow(5)
    val idleTimeoutSeconds: StateFlow<Int> = _idleTimeoutSeconds.asStateFlow()

    private val _highHzTarget = MutableStateFlow(120)
    val highHzTarget: StateFlow<Int> = _highHzTarget.asStateFlow()

    private val _lowHzTarget = MutableStateFlow(60)
    val lowHzTarget: StateFlow<Int> = _lowHzTarget.asStateFlow()

    private val _isPsmBypassEnabled = MutableStateFlow(true)
    val isPsmBypassEnabled: StateFlow<Boolean> = _isPsmBypassEnabled.asStateFlow()

    private val _isSmartChargingEnabled = MutableStateFlow(false)
    val isSmartChargingEnabled: StateFlow<Boolean> = _isSmartChargingEnabled.asStateFlow()

    private val _isLowBatteryDefenderEnabled = MutableStateFlow(true)
    val isLowBatteryDefenderEnabled: StateFlow<Boolean> = _isLowBatteryDefenderEnabled.asStateFlow()

    private val _isThermalProtectionEnabled = MutableStateFlow(true)
    val isThermalProtectionEnabled: StateFlow<Boolean> = _isThermalProtectionEnabled.asStateFlow()

    private val _isHudEnabled = MutableStateFlow(false)
    val isHudEnabled: StateFlow<Boolean> = _isHudEnabled.asStateFlow()

    private val _isStatusBarIndicatorEnabled = MutableStateFlow(true)
    val isStatusBarIndicatorEnabled: StateFlow<Boolean> = _isStatusBarIndicatorEnabled.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(false)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _appLanguage = MutableStateFlow(LanguageHelper.getCurrentLanguage())
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _appProfiles = MutableStateFlow<Map<String, ProfileMode>>(DEFAULT_VIDEO_APPS)
    val appProfiles: StateFlow<Map<String, ProfileMode>> = _appProfiles.asStateFlow()

    init {
        repositoryScope.launch {
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Log.e(TAG, "Error reading Preferences DataStore", exception)
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }
                .collectLatest { prefs ->
                    val modeStr = prefs[KEY_OPERATION_MODE] ?: OperationMode.ADAPTIVE.name
                    val mode = try {
                        OperationMode.valueOf(modeStr)
                    } catch (e: Exception) {
                        OperationMode.ADAPTIVE
                    }
                    _operationMode.value = mode
                    _isAdaptiveEnabled.value = prefs[KEY_ADAPTIVE_ENABLED]
                        ?: (mode == OperationMode.ADAPTIVE || mode == OperationMode.ECO)

                    _idleTimeoutSeconds.value = prefs[KEY_IDLE_TIMEOUT_SECONDS] ?: 5
                    _highHzTarget.value = prefs[KEY_HIGH_HZ_TARGET] ?: 120
                    _lowHzTarget.value = prefs[KEY_LOW_HZ_TARGET] ?: 60
                    _isPsmBypassEnabled.value = prefs[KEY_PSM_BYPASS] ?: true
                    _isSmartChargingEnabled.value = prefs[KEY_SMART_CHARGING] ?: false
                    _isLowBatteryDefenderEnabled.value = prefs[KEY_LOW_BATTERY_DEFENDER] ?: true
                    _isThermalProtectionEnabled.value = prefs[KEY_THERMAL_PROTECTION] ?: true
                    _isHudEnabled.value = prefs[KEY_HUD_ENABLED] ?: false
                    _isStatusBarIndicatorEnabled.value = prefs[KEY_STATUS_BAR_INDICATOR_ENABLED] ?: true
                    _isOnboardingCompleted.value = prefs[KEY_ONBOARDING_COMPLETED] ?: false
                    _appLanguage.value = prefs[KEY_APP_LANGUAGE] ?: LanguageHelper.getCurrentLanguage()

                    // Load app profile overrides
                    val profilesMap = HashMap<String, ProfileMode>(DEFAULT_VIDEO_APPS)
                    for ((key, value) in prefs.asMap()) {
                        if (key.name.startsWith(PREFIX_APP_PROFILE) && value is String) {
                            val pkg = key.name.removePrefix(PREFIX_APP_PROFILE)
                            try {
                                profilesMap[pkg] = ProfileMode.valueOf(value)
                            } catch (e: Exception) {
                                Log.w(TAG, "Unknown profile mode $value for $pkg", e)
                            }
                        }
                    }
                    _appProfiles.value = profilesMap
                }
        }
    }

    fun setOperationMode(mode: OperationMode) {
        _operationMode.value = mode
        val adaptive = (mode == OperationMode.ADAPTIVE || mode == OperationMode.ECO)
        _isAdaptiveEnabled.value = adaptive

        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_OPERATION_MODE] = mode.name
                preferences[KEY_ADAPTIVE_ENABLED] = adaptive
            }
        }
    }

    fun setAdaptiveEnabled(enabled: Boolean) {
        val newMode = if (enabled) OperationMode.ADAPTIVE else OperationMode.FIXED_60
        setOperationMode(newMode)
    }

    fun setIdleTimeoutSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(1, 60)
        _idleTimeoutSeconds.value = clamped
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_IDLE_TIMEOUT_SECONDS] = clamped
            }
        }
    }

    fun setHighHzTarget(hz: Int) {
        _highHzTarget.value = hz
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_HIGH_HZ_TARGET] = hz
            }
        }
    }

    fun setLowHzTarget(hz: Int) {
        _lowHzTarget.value = hz
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_LOW_HZ_TARGET] = hz
            }
        }
    }

    fun setPsmBypassEnabled(enabled: Boolean) {
        _isPsmBypassEnabled.value = enabled
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_PSM_BYPASS] = enabled
            }
        }
    }

    fun setSmartChargingEnabled(enabled: Boolean) {
        _isSmartChargingEnabled.value = enabled
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_SMART_CHARGING] = enabled
            }
        }
    }

    fun setLowBatteryDefenderEnabled(enabled: Boolean) {
        _isLowBatteryDefenderEnabled.value = enabled
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_LOW_BATTERY_DEFENDER] = enabled
            }
        }
    }

    fun setThermalProtectionEnabled(enabled: Boolean) {
        _isThermalProtectionEnabled.value = enabled
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_THERMAL_PROTECTION] = enabled
            }
        }
    }

    fun setHudEnabled(enabled: Boolean) {
        _isHudEnabled.value = enabled
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_HUD_ENABLED] = enabled
            }
        }
    }

    fun setStatusBarIndicatorEnabled(enabled: Boolean) {
        _isStatusBarIndicatorEnabled.value = enabled
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_STATUS_BAR_INDICATOR_ENABLED] = enabled
            }
        }
    }

    fun setAppProfileMode(packageName: String, mode: ProfileMode) {
        val current = _appProfiles.value.toMutableMap()
        val prefKey = stringPreferencesKey(PREFIX_APP_PROFILE + packageName)

        if (mode == ProfileMode.ADAPTIVE && !DEFAULT_VIDEO_APPS.containsKey(packageName)) {
            current.remove(packageName)
            repositoryScope.launch {
                dataStore.edit { preferences ->
                    preferences.remove(prefKey)
                }
            }
        } else {
            current[packageName] = mode
            repositoryScope.launch {
                dataStore.edit { preferences ->
                    preferences[prefKey] = mode.name
                }
            }
        }
        _appProfiles.value = current
    }

    fun getProfileModeForApp(packageName: String): ProfileMode {
        return _appProfiles.value[packageName] ?: DEFAULT_VIDEO_APPS[packageName] ?: ProfileMode.ADAPTIVE
    }

    fun setOnboardingCompleted(completed: Boolean) {
        _isOnboardingCompleted.value = completed
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_ONBOARDING_COMPLETED] = completed
            }
        }
    }

    fun setAppLanguage(langCode: String) {
        _appLanguage.value = langCode
        LanguageHelper.setLanguage(langCode)
        repositoryScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_APP_LANGUAGE] = langCode
            }
        }
    }

    companion object {
        private const val TAG = "SettingsRepository"
        const val PREFS_NAME = "adaptive_hz_settings"

        val KEY_OPERATION_MODE = stringPreferencesKey("key_operation_mode")
        val KEY_ADAPTIVE_ENABLED = booleanPreferencesKey("key_adaptive_enabled")
        val KEY_IDLE_TIMEOUT_SECONDS = intPreferencesKey("key_idle_timeout_seconds")
        val KEY_HIGH_HZ_TARGET = intPreferencesKey("key_high_hz_target")
        val KEY_LOW_HZ_TARGET = intPreferencesKey("key_low_hz_target")
        val KEY_PSM_BYPASS = booleanPreferencesKey("key_psm_bypass")
        val KEY_SMART_CHARGING = booleanPreferencesKey("key_smart_charging")
        val KEY_LOW_BATTERY_DEFENDER = booleanPreferencesKey("key_low_battery_defender")
        val KEY_THERMAL_PROTECTION = booleanPreferencesKey("key_thermal_protection")
        val KEY_HUD_ENABLED = booleanPreferencesKey("key_hud_enabled")
        val KEY_STATUS_BAR_INDICATOR_ENABLED = booleanPreferencesKey("key_status_bar_indicator_enabled")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("key_onboarding_completed")
        val KEY_APP_LANGUAGE = stringPreferencesKey("key_app_language")
        const val PREFIX_APP_PROFILE = "profile_"

        // Default profiles for video apps and camera (lock to 60Hz to save battery)
        val DEFAULT_VIDEO_APPS = mapOf(
            "com.google.android.youtube" to ProfileMode.FORCE_60,
            "ru.rutube.app" to ProfileMode.FORCE_60,
            "com.vkontakte.android" to ProfileMode.FORCE_60,
            "com.vk.video" to ProfileMode.FORCE_60,
            "com.vk.vkvideo" to ProfileMode.FORCE_60,
            "com.samsung.android.video" to ProfileMode.FORCE_60,
            "ru.kinopoisk" to ProfileMode.FORCE_60,
            "com.sec.android.app.camera" to ProfileMode.FORCE_60,
            "org.videolan.vlc" to ProfileMode.FORCE_60,
            "com.mxtech.videoplayer.ad" to ProfileMode.FORCE_60,
            "com.mxtech.videoplayer.pro" to ProfileMode.FORCE_60,
            "com.netflix.mediaclient" to ProfileMode.FORCE_60
        )

        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
