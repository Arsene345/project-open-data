package fr.enzor.projectopendata.modele

import com.google.gson.annotations.SerializedName

data class ResulteVote(
    @SerializedName("total_count") val totalCount: Int = 0,
    @SerializedName("results") val results: List<Vote> = emptyList()
)
