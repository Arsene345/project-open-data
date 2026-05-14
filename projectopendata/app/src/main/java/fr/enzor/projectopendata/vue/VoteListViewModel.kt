package fr.enzor.projectopendata.vue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn

class VoteListViewModel : ViewModel() {
    val votesFlow = Pager(
        config = PagingConfig(
            pageSize = 10,
            initialLoadSize = 20,
            enablePlaceholders = true,
        ),
        pagingSourceFactory = { VotePagingSource() },
    ).flow.cachedIn(viewModelScope)
}

