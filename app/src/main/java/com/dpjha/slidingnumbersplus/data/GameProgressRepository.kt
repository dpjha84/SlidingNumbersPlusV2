package com.dpjha.slidingnumbersplus.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dpjha.slidingnumbersplus.game.GameProgress
import com.dpjha.slidingnumbersplus.game.PuzzleEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File

private val Context.progressDataStore by preferencesDataStore(name = "game_progress")

class GameProgressRepository(private val context: Context) {
    suspend fun load(): GameProgress {
        val preferences = context.progressDataStore.data.first()
        preferences[PROGRESS_JSON]?.let { return decode(it) }
        if (preferences[LEGACY_MIGRATION_DONE] == true) return GameProgress()

        val legacy = withContext(Dispatchers.IO) { readLegacyProgress() }
        val restored = legacy?.normalized() ?: GameProgress()
        context.progressDataStore.edit {
            it[PROGRESS_JSON] = encode(restored)
            it[LEGACY_MIGRATION_DONE] = true
        }
        return restored
    }

    suspend fun save(progress: GameProgress) {
        val normalized = progress.normalized()
        context.progressDataStore.edit {
            it[PROGRESS_JSON] = encode(normalized)
            it[LEGACY_MIGRATION_DONE] = true
        }
    }

    private fun readLegacyProgress(): GameProgress? {
        val sharedPreferencesDirectory = File(context.applicationInfo.dataDir, "shared_prefs")
        val preferenceFiles = sharedPreferencesDirectory.listFiles() ?: return null
        for (file in preferenceFiles) {
            if (!file.name.endsWith(".xml")) continue
            val preferenceName = file.name.removeSuffix(".xml")
            val json = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
                .getString(LEGACY_KEY, null) ?: continue
            try {
                val objectValue = JSONObject(json)
                val level = objectValue.optInt("Level", 1).coerceAtLeast(1)
                val rawSequence = objectValue.optJSONArray("Sequence") ?: JSONArray()
                val sequence = mutableListOf<Int>()
                for (index in 0 until rawSequence.length()) {
                    val tile = rawSequence.optString(index)
                    sequence += if (tile.isBlank()) PuzzleEngine.EMPTY else tile.toIntOrNull() ?: return null
                }
                return GameProgress(
                    level = level,
                    maxLevel = objectValue.optInt("MaxLevel", 1),
                    starCount = objectValue.optInt("StarCount", 0),
                    movesLeft = objectValue.optInt("MovesLeft", -1),
                    sequence = sequence,
                    soundOn = objectValue.optBoolean("SoundOn", true),
                    musicOn = objectValue.optBoolean("MusicOn", false),
                    levelsCompleted = (objectValue.optInt("MaxLevel", 1) - 1).coerceAtLeast(0),
                )
            } catch (exception: JSONException) {
                Log.w(TAG, "Ignoring malformed legacy game save in ${file.name}", exception)
            }
        }
        return null
    }

    private fun decode(json: String): GameProgress {
        return try {
            val objectValue = JSONObject(json)
            val sequenceJson = objectValue.optJSONArray("sequence") ?: JSONArray()
            val sequence = buildList {
                for (index in 0 until sequenceJson.length()) add(sequenceJson.optInt(index))
            }
            GameProgress(
                level = objectValue.optInt("level", 1),
                maxLevel = objectValue.optInt("maxLevel", 1),
                starCount = objectValue.optInt("starCount", 0),
                movesLeft = objectValue.optInt("movesLeft", -1),
                sequence = sequence,
                soundOn = objectValue.optBoolean("soundOn", true),
                musicOn = objectValue.optBoolean("musicOn", false),
                totalScore = objectValue.optLong("totalScore", 0),
                levelsCompleted = objectValue.optInt("levelsCompleted", 0),
                bestStars = objectValue.optInt("bestStars", 0),
                hapticsOn = objectValue.optBoolean("hapticsOn", true),
                reducedMotion = objectValue.optBoolean("reducedMotion", false),
            ).normalized()
        } catch (exception: JSONException) {
            Log.w(TAG, "Ignoring malformed game save", exception)
            GameProgress()
        }
    }

    private fun encode(progress: GameProgress): String {
        val objectValue = JSONObject()
            .put("level", progress.level)
            .put("maxLevel", progress.maxLevel)
            .put("starCount", progress.starCount)
            .put("movesLeft", progress.movesLeft)
            .put("sequence", JSONArray(progress.sequence))
            .put("soundOn", progress.soundOn)
            .put("musicOn", progress.musicOn)
            .put("totalScore", progress.totalScore)
            .put("levelsCompleted", progress.levelsCompleted)
            .put("bestStars", progress.bestStars)
            .put("hapticsOn", progress.hapticsOn)
            .put("reducedMotion", progress.reducedMotion)
        return objectValue.toString()
    }

    private companion object {
        const val TAG = "GameProgressStore"
        const val LEGACY_KEY = "snp_v1"
        val PROGRESS_JSON = stringPreferencesKey("progress_json")
        val LEGACY_MIGRATION_DONE = booleanPreferencesKey("legacy_migration_done")
    }
}
