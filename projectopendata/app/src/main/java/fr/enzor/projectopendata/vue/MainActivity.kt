package fr.enzor.projectopendata.vue

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.clustering.rememberClusterManager
import com.google.maps.android.compose.clustering.rememberClusterRenderer
import com.google.maps.android.compose.rememberCameraPositionState
import fr.enzor.projectopendata.api.VoteApiManager
import fr.enzor.projectopendata.modele.Vote
import fr.enzor.projectopendata.ui.theme.ProjectOpenDataTheme
import fr.enzor.projectopendata.utils.PreferencesHelper


class MainActivity : ComponentActivity() {
    private var latestKnownVotes: List<Vote> = emptyList()
    private var initialCachedVotes: List<Vote> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (PreferencesHelper.isFirstLaunch(this)) {
            val Intent = Intent(this, PreferenceActivity::class.java)
            startActivity(Intent)
            PreferencesHelper.setFirstLaunchDone(this)
        }
        initialCachedVotes = loadVotes(this)
        latestKnownVotes = initialCachedVotes
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var lightTheme by remember { mutableStateOf(PreferencesHelper.getLightTheme(context)) }

            val lifecycleOwner= LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        lightTheme= PreferencesHelper.getLightTheme(context)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            ProjectOpenDataTheme (
                darkTheme = !lightTheme,
                dynamicColor = true
            ){
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainActivityContent(
                        modifier = Modifier.padding(innerPadding),
                        initialCachedVotes = initialCachedVotes,
                        onLatestVotesChanged = { votes ->
                            if (votes.isNotEmpty()) {
                                latestKnownVotes = votes
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (latestKnownVotes.isNotEmpty()) {
            saveVotes(this, latestKnownVotes)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivityContent(
    modifier: Modifier = Modifier,
    initialCachedVotes: List<Vote> = emptyList(),
    onLatestVotesChanged: (List<Vote>) -> Unit = {}
) {
    val isConnected = getConnectedState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentPage by rememberSaveable { mutableStateOf(NavigationKeys.VOTE_LIST) }
    var selectedVote by remember { mutableStateOf<Vote?>(null) }
    var lastKnownVotes by remember { mutableStateOf(initialCachedVotes) }
    var favoriteIds by remember { mutableStateOf(PreferencesHelper.getFavoriteIds(context)) }
    var onlyFavorites by remember { mutableStateOf(PreferencesHelper.getOnlyFavorites(context)) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                // Re-synchronise les prefs apres retour de PreferenceActivity.
                onlyFavorites = PreferencesHelper.getOnlyFavorites(context)
                favoriteIds = PreferencesHelper.getFavoriteIds(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }



    fun updateLastKnownVotes(candidates: List<Vote>) {
        if (candidates.isEmpty()) return
        if (candidates.size >= lastKnownVotes.size) {
            lastKnownVotes = candidates
            onLatestVotesChanged(candidates)
        }
    }

    LaunchedEffect(isConnected) {
        if (!isConnected) {
            Log.w("VoteScreen", "Aucune connexion internet detectee")
            Toast.makeText(context, "Aucune connexion internet detectee", Toast.LENGTH_LONG).show()
        }else{
            Log.d("VoteScreen", "Connexion internet detectee, chargement des votes...")
            Toast.makeText(context, "Connexion internet detectee, chargement des votes...", Toast.LENGTH_SHORT).show()
        }
    }


    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Project Open Data") },

                actions = {
                    IconButton(onClick = {
                        val intent = Intent(context, PreferenceActivity::class.java)
                        ContextCompat.startActivity(context, intent, null)
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Préférences")
                    }
                }

            )
        },
        bottomBar = {
            NavigationBar {
                NavigationKeys.entries.forEach { key ->
                    NavigationBarItem(
                        icon = {
                            androidx.compose.material3.Icon(
                                imageVector = key.Icon,
                                contentDescription = key.Description
                            )
                        },
                        label = { Text(key.label) },
                        selected = currentPage == key,
                        onClick = { currentPage = key }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            when (currentPage) {
                NavigationKeys.VOTE_LIST -> VoteScreen(
                    modifier = Modifier,
                    isConnected = isConnected,
                    fallbackVotes = lastKnownVotes,
                    favoriteIds = favoriteIds,
                    onToggleFavorite = { vote ->
                        PreferencesHelper.toggleFavorite(context, vote.id)
                        favoriteIds = PreferencesHelper.getFavoriteIds(context)
                    },
                    onSnapshotVotes = { snapshot -> updateLastKnownVotes(snapshot) },
                    onVoteClick = { vote ->
                        selectedVote = vote
                    }

                )
                NavigationKeys.VOTE_FAV -> FavScreen(modifier = Modifier.fillMaxSize(),
                    isConnected = isConnected,
                    fallbackVotes = lastKnownVotes,
                    onlyFavorites = onlyFavorites,
                    favoriteIds = favoriteIds,
                    onToggleFavorite = { vote ->
                        PreferencesHelper.toggleFavorite(context, vote.id)
                        favoriteIds = PreferencesHelper.getFavoriteIds(context)
                    }

                )

                NavigationKeys.VOTE_MAP -> VoteMapScreen(
                    modifier = Modifier,
                    isConnected = isConnected,
                    initialVotes = lastKnownVotes,
                    onVotesLoaded = { votes -> updateLastKnownVotes(votes) },
                    onVoteClick = { vote -> selectedVote = vote },
                    favoriteIds=favoriteIds,
                    onlyFavorites = onlyFavorites
                )
            }
        }
    }

    selectedVote?.let { vote ->
        VoteDetailsDialog(
            vote = vote,
            onDismiss = { selectedVote = null }
        )
    }
}

@Composable
fun VoteScreen(
    modifier: Modifier = Modifier,
    isConnected: Boolean,
    fallbackVotes: List<Vote> = emptyList(),
    onSnapshotVotes: (List<Vote>) -> Unit = {},
    onVoteClick: (Vote) -> Unit = {},
    favoriteIds: Set<String>,
    onToggleFavorite: (Vote) -> Unit,)
{
    val vm: VoteListViewModel = viewModel()
    val lazyItems = vm.votesFlow.collectAsLazyPagingItems()

    // Relance le paging uniquement quand la connexion revient.
    LaunchedEffect(isConnected) {
        if (isConnected) {
            lazyItems.refresh()
        }
    }

    LaunchedEffect(lazyItems.itemCount) {
        val loadedVotes = lazyItems.itemSnapshotList.items
        if (loadedVotes.isNotEmpty()) {
            onSnapshotVotes(loadedVotes)
        }
    }

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        when (val refresh = lazyItems.loadState.refresh) {
            is LoadState.Loading -> Text("Chargement des votes...", modifier = Modifier.padding(top = 16.dp))
            is LoadState.Error -> {
                if (!isConnected && fallbackVotes.isNotEmpty()) {
                    Text("Hors ligne: donnees locales affichees", modifier = Modifier.padding(top = 16.dp))
                    OfflineVotesList(
                        votes = fallbackVotes,
                        favoriteIds = favoriteIds,
                        onVoteClick = onVoteClick,
                        onToggleFavorite = onToggleFavorite
                    )
                } else {
                    Text(
                        "Erreur: ${refresh.error.localizedMessage ?: "Erreur inconnue"}",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
            is LoadState.NotLoading -> {
                if (lazyItems.itemCount == 0) {
                    val message = if (!isConnected && fallbackVotes.isNotEmpty()) {
                        "Hors ligne: donnees locales affichees"
                    } else if (isConnected) {
                        "Aucun vote recu (liste vide)"
                    } else {
                        "Hors ligne: aucune donnee locale disponible"
                    }
                    Text(message, modifier = Modifier.padding(top = 16.dp))

                    if (!isConnected && fallbackVotes.isNotEmpty()) {
                        OfflineVotesList(
                            votes = fallbackVotes,
                            favoriteIds = favoriteIds,
                            onVoteClick = onVoteClick,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                } else {
                    Text(
                        text = "Nombre de bureaux de vote charges: ${lazyItems.itemCount}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    LazyColumn {
                        items(
                            count = lazyItems.itemCount,
                            key = { index -> lazyItems[index]?.id ?: "placeholder_$index" }
                        ) { index ->
                            val vote = lazyItems[index]

                            if (vote == null) {
                                Text("Chargement...", modifier = Modifier.padding(vertical = 8.dp))
                                return@items
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        Log.d(
                                            "VoteScreen",
                                            "Item clique index=$index, id=${vote.id}"
                                        )
                                        onVoteClick(vote)
                                    },
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "#${index + 1} - ${vote.libelle ?: "Sans libelle"}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(text = "Commune: ${vote.com_name ?: "-"} (${vote.postal_code ?: "-"})")
                                    Text(text = "Departement: ${vote.dep_name ?: "-"} (${vote.dep_code ?: "-"})")
                                    Text(text = "Region: ${vote.reg_name ?: "-"}")
                                    if (vote.adresse != null) {
                                        Text(text = "Adresse: ${vote.adresse}")
                                    }
                                }
                                IconButton(onClick = {onToggleFavorite(vote)}) {
                                    androidx.compose.material3.Icon(
                                        imageVector = if (favoriteIds.contains(vote.id)) {
                                            androidx.compose.material.icons.Icons.Filled.Favorite
                                        } else {
                                            androidx.compose.material.icons.Icons.Outlined.FavoriteBorder
                                        },
                                        contentDescription = "Toggle Favorite"
                                    )
                                }
                            }
                        }

                        if (lazyItems.loadState.append is LoadState.Loading) {
                            item {
                                Text("Chargement de plus de votes...", modifier = Modifier.padding(vertical = 12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineVotesList(
    votes: List<Vote>,
    favoriteIds: Set<String>,
    onVoteClick: (Vote) -> Unit,
    onToggleFavorite: (Vote) -> Unit
) {
    LazyColumn {
        items(items = votes, key = { vote -> vote.id }) { vote ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onVoteClick(vote) },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = vote.libelle ?: "Sans libelle", style = MaterialTheme.typography.titleSmall)
                    Text(text = "Commune: ${vote.com_name ?: "-"} (${vote.postal_code ?: "-"})")
                    Text(text = "Departement: ${vote.dep_name ?: "-"} (${vote.dep_code ?: "-"})")
                    Text(text = "Region: ${vote.reg_name ?: "-"}")
                }
                IconButton(onClick = { onToggleFavorite(vote) }) {
                    androidx.compose.material3.Icon(
                        imageVector = if (favoriteIds.contains(vote.id)) {
                            androidx.compose.material.icons.Icons.Filled.Favorite
                        } else {
                            androidx.compose.material.icons.Icons.Outlined.FavoriteBorder
                        },
                        contentDescription = "Toggle Favorite"
                    )
                }
            }
        }
    }
}

@Composable
fun FavScreen(
    modifier: Modifier = Modifier,
    isConnected: Boolean,
    fallbackVotes: List<Vote> = emptyList(),
    onlyFavorites: Boolean,
    favoriteIds: Set<String>,
    onToggleFavorite: (Vote) -> Unit
) {
    val vm: VoteListViewModel = viewModel()
    val lazyItems = vm.votesFlow.collectAsLazyPagingItems()

    LaunchedEffect(isConnected) {
        if (isConnected) lazyItems.refresh()
    }

    val fallbackFavorites = remember(fallbackVotes, favoriteIds) {
        fallbackVotes.filter { favoriteIds.contains(it.id) }
    }
    val showFallback = !isConnected && lazyItems.itemCount == 0 && fallbackFavorites.isNotEmpty()

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Bureaux de vote favoris",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        if (!showFallback && lazyItems.itemCount == 0) {
            val msg = if (!isConnected) {
                "Hors ligne: aucun favori disponible en cache"
            } else {
                "Aucun favori"
            }
            Text(msg, modifier = Modifier.padding(vertical = 8.dp))
        }

        if (showFallback) {
            LazyColumn {
                items(items = fallbackFavorites, key = { vote -> vote.id }) { vote ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = vote.libelle ?: "Sans libelle", style = MaterialTheme.typography.titleSmall)
                            Text(text = "Commune: ${vote.com_name ?: "-"} (${vote.postal_code ?: "-"})")
                            Text(text = "Departement: ${vote.dep_name ?: "-"} (${vote.dep_code ?: "-"})")
                            Text(text = "Region: ${vote.reg_name ?: "-"}")
                        }
                        IconButton(onClick = { onToggleFavorite(vote) }) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.Favorite,
                                contentDescription = "Remove Favorite"
                            )
                        }
                    }
                }
            }
            return@Column
        }

        LazyColumn {
            items(count = lazyItems.itemCount, key = { index -> lazyItems[index]?.id ?: "placeholder_$index" }) { index ->
                val vote = lazyItems[index] ?: return@items
                if (favoriteIds.contains(vote.id)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = vote.libelle ?: "Sans libelle", style = MaterialTheme.typography.titleSmall)
                            Text(text = "Commune: ${vote.com_name ?: "-"} (${vote.postal_code ?: "-"})")
                            Text(text = "Departement: ${vote.dep_name ?: "-"} (${vote.dep_code ?: "-"})")
                            Text(text = "Region: ${vote.reg_name ?: "-"}")
                        }
                        IconButton(onClick = { onToggleFavorite(vote) }) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.Favorite,
                                contentDescription = "Remove Favorite"
                            )
                        }
                    }
                }
            }
        }
    }
}


@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun VoteMapScreen(
    modifier: Modifier = Modifier,
    isConnected: Boolean,
    initialVotes: List<Vote> = emptyList(),
    onVotesLoaded: (List<Vote>) -> Unit = {},
    onVoteClick: (Vote) -> Unit = {},
    focusedVoteId: String? = null,
    onlyFavorites: Boolean,
    favoriteIds: Set<String>,
) {
    var votes by remember { mutableStateOf(initialVotes) }

    val france = LatLng(46.603354, 1.888334)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(france, 5f)
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = false),
        uiSettings = MapUiSettings(zoomControlsEnabled = true)
    ) {
        LaunchedEffect(isConnected) {
            if (!isConnected) return@LaunchedEffect

            try {
                val networkVotes = VoteApiManager.fetchAllVotes()
                votes = networkVotes
                onVotesLoaded(networkVotes)
            } catch (_: Exception) {
                // Garde le cache affiche sur la carte en cas d'erreur reseau.
            }
        }

        val displayedVotes = remember(votes, onlyFavorites, favoriteIds) {
            if (onlyFavorites) {
                votes.filter { favoriteIds.contains(it.id) }
            } else {
                votes
            }
        }

        val voteClusterItem = remember(displayedVotes) {
            displayedVotes.mapNotNull { vote ->
                val lat = vote.location?.lat ?: return@mapNotNull null
                val lng = vote.location.lon ?: return@mapNotNull null
                VoteClusterItem(
                    vote = vote,
                    itemPosition = LatLng(lat, lng)
                )
            }
        }

        val clusterManager = rememberClusterManager<VoteClusterItem>()
        val clusterRenderer = rememberClusterRenderer(clusterManager)

        SideEffect {
            clusterManager ?: return@SideEffect

            if (clusterRenderer != null && clusterManager.renderer != clusterRenderer) {
                clusterManager.renderer = clusterRenderer
            }

            (clusterManager.renderer as? DefaultClusterRenderer<VoteClusterItem>)?.minClusterSize = 3

            clusterManager.setOnClusterItemClickListener { item ->
                Log.d("VoteMapScreen", "Cluster item clicked: ${item.vote.id}")
                onVoteClick(item.vote)
                true
            }
        }

        if (clusterManager != null) {
            Clustering(items = voteClusterItem, clusterManager = clusterManager)
        }
    }
}

@Composable
fun getConnectedState(): Boolean {
    val context = LocalContext.current
    val connectivityManager = remember(context) {
        context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    var isConnected by remember { mutableStateOf(hasInternetCapability(connectivityManager)) }

    DisposableEffect(connectivityManager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                isConnected = hasInternetCapability(connectivityManager)
            }

            override fun onLost(network: android.net.Network) {
                isConnected = hasInternetCapability(connectivityManager)
            }

            override fun onCapabilitiesChanged(
                network: android.net.Network,
                networkCapabilities: NetworkCapabilities
            ) {
                isConnected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }

            override fun onUnavailable() {
                isConnected = false
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        isConnected = hasInternetCapability(connectivityManager)
        connectivityManager.registerNetworkCallback(request, callback)

        onDispose {
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        }
    }

    return isConnected
}

private fun hasInternetCapability(connectivityManager: ConnectivityManager): Boolean {
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

data class VoteClusterItem(
    val vote: Vote,
    val itemPosition: LatLng,
) : ClusterItem {
    override fun getPosition(): LatLng = itemPosition
    override fun getTitle(): String = vote.libelle ?: "Sans libelle"
    override fun getSnippet(): String = "${vote.com_name ?: "-"} (${vote.postal_code ?: "-"})"
    override fun getZIndex(): Float = 0f
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ProjectOpenDataTheme {

    }
}

@Composable
private fun VoteDetailsDialog(
    vote: Vote,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(vote.libelle ?: "Sans libelle") },
        text = {
            Column {
                Text("Commune: ${vote.com_name ?: "-"} (${vote.postal_code ?: "-"})")
                Text("Departement: ${vote.dep_name ?: "-"} (${vote.dep_code ?: "-"})")
                Text("Region: ${vote.reg_name ?: "-"}")
                vote.adresse?.let { Text("Adresse: $it") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}
