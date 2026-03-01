package com.autorizz.dating.prefs

import com.autorizz.dating.db.SwipePreferenceDao
import com.autorizz.dating.db.SwipePreferenceEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val dao: SwipePreferenceDao
) {
    suspend fun get(key: String): String? = dao.getByKey(key)?.value

    suspend fun getAll(): Map<String, String> =
        dao.getAll().associate { it.key to it.value }

    suspend fun set(key: String, value: String) {
        val existing = dao.getByKey(key)
        dao.upsert(
            SwipePreferenceEntity(
                id = existing?.id ?: 0,
                key = key,
                value = value,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun delete(key: String) = dao.delete(key)

    suspend fun buildPrefsContext(): String {
        val prefs = getAll()
        if (prefs.isEmpty()) return "No swipe preferences set."
        return buildString {
            appendLine("User's swipe preferences:")
            prefs.forEach { (key, value) ->
                appendLine("- ${key.replace("_", " ")}: $value")
            }
        }
    }
}
