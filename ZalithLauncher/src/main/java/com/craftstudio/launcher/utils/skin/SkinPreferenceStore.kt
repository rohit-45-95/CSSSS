package com.craftstudio.launcher.utils.skin

import android.content.Context
import com.craftstudio.launcher.auth.offline.TextureModel

object SkinPreferenceStore {
    private const val PREFS_NAME = "local_skin_prefs"
    private const val KEY_SKIN_PREFIX = "skin_path_"
    private const val KEY_CAPE_PREFIX = "cape_path_"
    private const val KEY_TEXTURE_MODEL_PREFIX = "texture_model_"

    /**
     * Save local skin file path for an account.
     */
    fun saveSkinPath(context: Context, username: String, path: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SKIN_PREFIX + username.lowercase(), path)
            .apply()
    }

    /**
     * Save local cape file path for an account.
     */
    fun saveCapePath(context: Context, username: String, path: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CAPE_PREFIX + username.lowercase(), path)
            .apply()
    }

    /**
     * Save texture model (STEVE or ALEX) for an account.
     */
    fun saveTextureModel(context: Context, username: String, model: TextureModel) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TEXTURE_MODEL_PREFIX + username.lowercase(), model.name)
            .apply()
    }

    /**
     * Get local skin file path for an account.
     */
    fun getSkinPath(context: Context, username: String): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SKIN_PREFIX + username.lowercase(), null)
    }

    /**
     * Get local cape file path for an account.
     */
    fun getCapePath(context: Context, username: String): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CAPE_PREFIX + username.lowercase(), null)
    }

    /**
     * Get texture model (STEVE or ALEX) for an account. Defaults to STEVE.
     */
    fun getTextureModel(context: Context, username: String): TextureModel {
        val modelStr = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TEXTURE_MODEL_PREFIX + username.lowercase(), null)
        return try {
            TextureModel.valueOf(modelStr ?: "STEVE")
        } catch (e: Exception) {
            TextureModel.STEVE
        }
    }

    /**
     * Clear all skin preferences for an account.
     */
    fun clearSkinPreferences(context: Context, username: String) {
        val key = username.lowercase()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SKIN_PREFIX + key)
            .remove(KEY_CAPE_PREFIX + key)
            .remove(KEY_TEXTURE_MODEL_PREFIX + key)
            .apply()
    }
}
