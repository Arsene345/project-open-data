package fr.enzor.projectopendata.vue

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fr.enzor.projectopendata.modele.Vote

private const val VOTE_CACHE_PREFS = "vote_cache_prefs"
private const val KEY_VOTES_JSON = "key_votes_json"
private const val KEY_VOTES_TIMESTAMP = "key_votes_timestamp"

private val gson = Gson()

fun saveVotes(context: Context, votes: List<Vote>) {
    val prefs = context.getSharedPreferences(VOTE_CACHE_PREFS, Context.MODE_PRIVATE)
    val votesJson = gson.toJson(votes)

    prefs.edit()
        .putString(KEY_VOTES_JSON, votesJson)
        .putLong(KEY_VOTES_TIMESTAMP, System.currentTimeMillis())
        .apply()
}

fun loadVotes(context: Context): List<Vote> {
    val prefs = context.getSharedPreferences(VOTE_CACHE_PREFS, Context.MODE_PRIVATE)
    val votesJson = prefs.getString(KEY_VOTES_JSON, null) ?: return emptyList()

    return runCatching {
        val type = object : TypeToken<List<Vote>>() {}.type
        gson.fromJson<List<Vote>>(votesJson, type) ?: emptyList()
    }.getOrDefault(emptyList())
}

fun clear(context: Context) {
    val prefs = context.getSharedPreferences(VOTE_CACHE_PREFS, Context.MODE_PRIVATE)
    prefs.edit()
        .remove(KEY_VOTES_JSON)
        .remove(KEY_VOTES_TIMESTAMP)
        .apply()
}