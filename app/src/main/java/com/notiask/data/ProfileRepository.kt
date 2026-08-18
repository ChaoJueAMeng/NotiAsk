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
    private val _defaultId = MutableStateFlow(preferences.getString(DEFAULT_ID, null))
    val defaultId: StateFlow<String?> = _defaultId

    fun defaultProfile(): ConfiguredProfile? {
        val id = _defaultId.value ?: return null
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
        val becomeDefault = makeDefault || _defaultId.value == null
        preferences.edit()
            .putString(PROFILES, json.encodeToString(profilesSerializer, updated))
            .putString(keyName(sanitized.id), cipher.encrypt(apiKey.trim()))
            .apply {
                if (becomeDefault) putString(DEFAULT_ID, sanitized.id)
            }
            .apply()
        _profiles.value = updated
        if (becomeDefault) _defaultId.value = sanitized.id
    }

    fun selectDefault(id: String) {
        require(_profiles.value.any { it.id == id })
        preferences.edit().putString(DEFAULT_ID, id).apply()
        _defaultId.value = id
    }

    fun delete(id: String) {
        val updated = _profiles.value.filterNot { it.id == id }
        preferences.edit().remove(keyName(id)).putString(PROFILES, json.encodeToString(profilesSerializer, updated)).apply()
        if (_defaultId.value == id) {
            val next = updated.firstOrNull()?.id
            preferences.edit().putString(DEFAULT_ID, next).apply()
            _defaultId.value = next
        }
        _profiles.value = updated
    }

    fun isDefault(id: String) = _defaultId.value == id

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
