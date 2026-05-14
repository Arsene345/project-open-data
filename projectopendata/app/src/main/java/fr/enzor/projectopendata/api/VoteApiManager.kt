package fr.enzor.projectopendata.api

import android.util.Log
import fr.enzor.projectopendata.modele.Vote
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class VotePageResult(
    val votes: List<Vote>,
    val totalCount: Int,
)

object VoteApiManager {
    private const val TAG = "API_TEST"

    val retrofit = Retrofit.Builder()
        .baseUrl("https://hub.huwise.com/api/explore/v2.1/catalog/datasets/elections-france-bureau-vote-2022/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(VoteApiService::class.java)

    suspend fun fetchVotesPage(limit: Int, offset: Int): VotePageResult {
        return try {
            val reponse = apiService.getVotes(limit, offset)
            if (reponse.isSuccessful) {
                val body = reponse.body()
                VotePageResult(
                    votes = body?.results ?: emptyList(),
                    totalCount = body?.totalCount ?: 0,
                )
            } else {
                val errorText = reponse.errorBody()?.string()
                Log.e(TAG, "Erreur HTTP page | code=${reponse.code()} | errorBody=$errorText")
                VotePageResult(emptyList(), 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de l'appel API page: ${e.message}", e)
            VotePageResult(emptyList(), 0)
        }
    }

    suspend fun fetchAllVotes(batchSize: Int = 100): List<Vote> {
        val allVotes = mutableListOf<Vote>()
        var offset = 0
        var totalCount: Int? = null

        while (true) {
            val page = fetchVotesPage(limit = batchSize, offset = offset)
            val votes = page.votes

            if (votes.isEmpty()) break

            allVotes.addAll(votes)
            offset += votes.size

            if (totalCount == null && page.totalCount > 0) {
                totalCount = page.totalCount
            }

            if (totalCount != null && offset >= totalCount) break
        }

        Log.d(TAG, "fetchAllVotes() -> total recu=${allVotes.size}")
        return allVotes
    }

    suspend fun fetchVotes(): List<Vote> {
        val requestUrl = retrofit.baseUrl().toString() + "records/?limit=10&offset=0"
        Log.d(TAG, "Debut appel API | url=$requestUrl")

        return try {
            val reponse = apiService.getVotes(60, 0)
            Log.d(
                TAG,
                "Reponse API | code=${reponse.code()} | successful=${reponse.isSuccessful} | message=${reponse.message()}"
            )

            if (reponse.isSuccessful) {
                val body = reponse.body()
                Log.d(TAG, "Body present=${body != null}")

                if (body != null) {
                    Log.d(TAG, "total_count=${body.totalCount}")
                    val listeVotes = body.results
                    Log.d(TAG, "Nombre de votes recus (results): ${listeVotes.size}")

                    // Splitter et afficher chaque vote individuellement
                    listeVotes.forEachIndexed { index, vote ->
                        Log.d(TAG, "Vote[$index] = $vote")
                    }

                    listeVotes
                } else {
                    Log.w(TAG, "Body null, retour liste vide")
                    emptyList()
                }
            } else {
                val errorText = reponse.errorBody()?.string()
                Log.e(TAG, "Erreur HTTP | code=${reponse.code()} | errorBody=$errorText")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de l'appel API: ${e.message}", e)
            emptyList()
        }
    }
}