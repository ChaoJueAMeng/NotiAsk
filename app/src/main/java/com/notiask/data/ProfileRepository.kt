package com.notiask.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class ProfileRepository(context: Context) {
    private val preferences = context.getSharedPreferences("notiask_config", Context.MODE_PRIVATE)
    private val cipher = KeystoreCipher()
    private val json = Json { ignoreUnknownKeys = true }
    private val profilesSerializer = ListSerializer(AiProfile.serializer())
    private val _profiles = MutableStateFlow(readProfiles())
    val profiles: StateFlow<List<AiProfile>> = _profiles

    fun defaultProfile(): ConfiguredProfile? {
        val id = preferences.getString(DEFAULT_ID, null) ?: return null
        val profile = _profiles.value.firstOrNull { it.id == id } ?: return null
        val encryptedKey = preferences.getString(keyName(id), null) ?: return null
        return runCatching { ConfiguredProfile(profile, cipher.decrypt(encryptedKey)) }.getOrNull()
    }

    fun save(profile: AiProfile, apiKey: String, makeDefault: Boolean) {
        require(apiKey.isNotBlank()) { "API Key 不能为空" }
        val sanitized = profile.copy(
            name = profile.name.ifBlank { profile.provider.displayName },
            baseUrl = profile.baseUrl.trim().trimEnd('/'),
            model = profile.model.trim()
        )
        require(sanitized.baseUrl.isNotBlank()) { "Base URL 不能为空" }
        require(sanitized.model.isNotBlank()) { "模型名称不能为空" }
        val updated = _profiles.value.filterNot { it.id == sanitized.id } + sanitized
        preferences.edit()
            .putString(PROFILES, json.encodeToString(profilesSerializer, updated))
            .putString(keyName(sanitized.id), cipher.encrypt(apiKey.trim()))
            .apply {
                if (makeDefault || preferences.getString(DEFAULT_ID, null) == null) putString(DEFAULT_ID, sanitized.id)
            }
            .apply()
        _profiles.value = updated
    }

    fun selectDefault(id: String) {
        require(_profiles.value.any { it.id == id })
        preferences.edit().putString(DEFAULT_ID, id).apply()
    }

    fun delete(id: String) {
        val updated = _profiles.value.filterNot { it.id == id }
        preferences.edit().remove(keyName(id)).putString(PROFILES, json.encodeToString(profilesSerializer, updated)).apply()
        if (preferences.getString(DEFAULT_ID, null) == id) {
            preferences.edit().putString(DEFAULT_ID, updated.firstOrNull()?.id).apply()
        }
        _profiles.value = updated
    }

    fun isDefault(id: String) = preferences.getString(DEFAULT_ID, null) == id

    fun apiKeyFor(id: String): String? = preferences.getString(keyName(id), null)?.let { encrypted ->
        runCatching { cipher.decrypt(encrypted) }.getOrNull()
    }

    private fun readProfiles(): List<AiProfile> = runCatching {
        json.decodeFromString(profilesSerializer, preferences.getString(PROFILES, "[]")!!)
    }.getOrDefault(emptyList())

    private fun keyName(id: String) = "encrypted_api_key_$id"

    private companion object {
        const val PROFILES = "profiles"
        const val DEFAULT_ID = "default_profile_id"
    }
}
