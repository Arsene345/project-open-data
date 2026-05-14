package fr.enzor.projectopendata.vue

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavigationKeys (val title: String,val label: String = title,val Icon: ImageVector,val Description: String) {
    VOTE_LIST("vote_list","Liste des votes", Icons.Filled.List,"Icône de liste de votes"),

    VOTE_FAV("vote_fav","Votes favoris", Icons.Filled.HowToVote,"Icône de votes favoris"),
    VOTE_MAP("vote_map","Carte", Icons.Filled.Map,"Icône de carte des votes")
}