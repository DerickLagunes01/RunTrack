package mx.edu.utez.runtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import mx.edu.utez.runtrack.ui.screens.RunTrackScreen
import mx.edu.utez.runtrack.ui.theme.RunTrackTheme

/**
 * Actividad principal de la aplicación RunTrack.
 * Configura la interfaz de usuario con Jetpack Compose y activa Edge-to-Edge.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RunTrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RunTrackScreen()
                }
            }
        }
    }
}
