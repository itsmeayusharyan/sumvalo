package com.acuranza.app.ui.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acuranza.app.di.AppContainer
import com.acuranza.app.model.GitHubReleaseClient
import com.acuranza.app.model.ReleaseEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/** UI state for the changelog screen. */
sealed class ChangelogUiState {
    data class Loading(val releases: List<ReleaseEntry>, val refreshing: Boolean) : ChangelogUiState()
    data class Success(val releases: List<ReleaseEntry>, val refreshing: Boolean, val fromCache: Boolean) : ChangelogUiState()
    data class Error(val releases: List<ReleaseEntry>, val exception: Exception, val refreshing: Boolean) : ChangelogUiState()
    data class Empty(val refreshing: Boolean) : ChangelogUiState()
}

/** ViewModel for the changelog screen. Fetches releases from GitHub, caches them offline,
 * and exposes UI state.
 *
 * [container] provides [AppContainer.releaseCache], which is initialised once in
 * [com.acuranza.app.SumvaloApp.onCreate].
 */
class ChangelogViewModel(
    private val container: AppContainer,
    private val client: GitHubReleaseClient = GitHubReleaseClient,
) : ViewModel() {

    private val cache = container.releaseCache

    private val _uiState = MutableStateFlow<ChangelogUiState>(ChangelogUiState.Empty(refreshing = false))
    val uiState: StateFlow<ChangelogUiState> = _uiState.asStateFlow()

    /** Whether a refresh is currently in progress. */
    val isRefreshing: Flow<Boolean>
        get() = _uiState.map { state ->
            when (state) {
                is ChangelogUiState.Loading -> state.refreshing
                is ChangelogUiState.Success -> state.refreshing
                is ChangelogUiState.Error -> state.refreshing
                is ChangelogUiState.Empty -> state.refreshing
            }
        }

    init {
        loadReleases(refreshing = false)
    }

    /** Load releases: try cache first, then network. Called on screen creation. */
    fun loadReleases(refreshing: Boolean) = viewModelScope.launch {
        val cached = cache.getCachedReleases()
        if (cached.isNotEmpty()) {
            _uiState.value = ChangelogUiState.Success(releases = cached, refreshing = refreshing, fromCache = true)
        } else {
            _uiState.value = ChangelogUiState.Loading(releases = emptyList(), refreshing = refreshing)
        }

        fetchAndCache()
    }

    /** User-initiated refresh: fetch from network, cache, and update UI. */
    fun refresh() = viewModelScope.launch {
        fetchAndCache(refreshing = true)
    }

    private fun fetchAndCache(refreshing: Boolean = false) = viewModelScope.launch {
        try {
            val releases = withContext(Dispatchers.IO) {
                client.fetchReleases()
            }
            if (releases.isNotEmpty()) {
                cache.saveReleases(releases)
                _uiState.value = ChangelogUiState.Success(
                    releases = releases,
                    refreshing = false,
                    fromCache = false,
                )
            } else {
                val current = _uiState.value
                when (current) {
                    is ChangelogUiState.Error -> _uiState.value = current
                    else -> _uiState.value = ChangelogUiState.Empty(refreshing = false)
                }
            }
        } catch (e: Exception) {
            val current = _uiState.value
            val cachedReleases = when (current) {
                is ChangelogUiState.Success -> current.releases
                is ChangelogUiState.Loading -> current.releases
                is ChangelogUiState.Error -> current.releases
                else -> emptyList()
            }
            _uiState.value = ChangelogUiState.Error(
                releases = cachedReleases,
                exception = e,
                refreshing = false,
            )
        }
    }
}
