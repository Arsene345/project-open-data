package fr.enzor.projectopendata.api

import fr.enzor.projectopendata.modele.ResulteVote
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface VoteApiService {
    @GET("records/")
    suspend fun getVotes(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Response<ResulteVote>
}