package fr.enzor.projectopendata.vue

import androidx.paging.PagingSource
import androidx.paging.PagingState
import fr.enzor.projectopendata.api.VoteApiManager
import fr.enzor.projectopendata.modele.Vote

class VotePagingSource : PagingSource<Int, Vote>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Vote> {
        return try {
            val offset = params.key ?: 0
            val limit = params.loadSize
            val page = VoteApiManager.fetchVotesPage(limit = limit, offset = offset)
            val data = page.votes

            val prevKey = if (offset == 0) null else (offset - limit).coerceAtLeast(0)
            val nextKey = when {
                data.isEmpty() -> null
                page.totalCount > 0 && offset + data.size >= page.totalCount -> null
                else -> offset + data.size
            }

            LoadResult.Page(
                data = data,
                prevKey = prevKey,
                nextKey = nextKey,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Vote>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val anchorPage = state.closestPageToPosition(anchorPosition) ?: return null

        return anchorPage.prevKey?.let { it + state.config.pageSize }
            ?: anchorPage.nextKey?.let { it - state.config.pageSize }
    }
}

