package fr.enzor.projectopendata.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

private const val PREFS_FILE = "vote_prefs"
private const val KEY_FAVORITES = "favorites_ids"
private const val KEY_ONLY_FAVORITES = "only_favorites"
private const val KEY_LIGHT_THEME = "light_theme"
private const val KEY_FIRST_LAUNCH_DONE = "first_launch_done"

object PreferencesHelper {
    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    fun getFavoriteIds(context: Context): Set<String> =
        (getPrefs(context).getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()).toSet()

    fun isFavorite(context: Context, voteId: String?): Boolean {
        if (voteId == null) return false
        return getFavoriteIds(context).contains(voteId)
    }

    fun toggleFavorite(context: Context, voteId: String?) {
        if (voteId == null) return
        val prefs = getPrefs(context)
        val current = prefs.getStringSet(KEY_FAVORITES, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (current.contains(voteId)) current.remove(voteId) else current.add(voteId)
        prefs.edit { putStringSet(KEY_FAVORITES, current) }
    }

    fun getOnlyFavorites(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_ONLY_FAVORITES, false)

    fun setOnlyFavorites(context: Context, value: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_ONLY_FAVORITES, value) }
    }

    fun getLightTheme(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_LIGHT_THEME, false)

    fun setLightTheme(context: Context, value: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_LIGHT_THEME, value) }
    }

    fun isFirstLaunch(context: Context): Boolean =
        !getPrefs(context).getBoolean(KEY_FIRST_LAUNCH_DONE, false)

    fun setFirstLaunchDone(context: Context) {
        getPrefs(context).edit { putBoolean(KEY_FIRST_LAUNCH_DONE, true) }
    }
}
