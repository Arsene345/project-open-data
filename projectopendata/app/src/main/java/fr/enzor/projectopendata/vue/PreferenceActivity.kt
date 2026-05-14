package fr.enzor.projectopendata.vue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fr.enzor.projectopendata.ui.theme.ProjectOpenDataTheme
import fr.enzor.projectopendata.utils.PreferencesHelper


class PreferenceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProjectOpenDataTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PreferenceScreenContent(onClose = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferenceScreenContent(onClose: () -> Unit) {
    val context = LocalContext.current
    var onlyFavorites by remember { mutableStateOf(PreferencesHelper.getOnlyFavorites(context)) }
    var lightTheme by remember { mutableStateOf(PreferencesHelper.getLightTheme(context)) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Préférences") })
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Afficher uniquement les favoris sur la carte",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = onlyFavorites,
                    onCheckedChange = { checked ->
                        onlyFavorites = checked
                        PreferencesHelper.setOnlyFavorites(context, checked)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thème clair (blanc)",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = lightTheme,
                    onCheckedChange = { checked ->
                        lightTheme = checked
                        PreferencesHelper.setLightTheme(context, checked)
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                Text("Fermer")
            }
        }
    }
}
