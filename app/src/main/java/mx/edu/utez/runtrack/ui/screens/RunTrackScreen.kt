package mx.edu.utez.runtrack.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.edu.utez.runtrack.ui.state.RunTrackUiState
import mx.edu.utez.runtrack.ui.viewmodel.RunViewModel
import java.util.Locale

/**
 * Pantalla principal de la aplicación RunTrack implementada con Jetpack Compose y Material 3.
 *
 * Muestra el estado del monitoreo deportivo, tarjetas estilizadas con los datos del
 * sensor en tiempo real y un botón prominente que cambia dinámicamente de color.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunTrackScreen(
    viewModel: RunViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Lanzador para solicitar permiso de notificaciones (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startTracking(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "RunTrack",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Monitoreo Deportivo de Bajo Consumo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Badge descriptivo de bajo consumo por hardware
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatterySaver,
                                contentDescription = "Bajo consumo",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "50Hz FIFO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta Principal de Monitoreo
            SensorDataCard(uiState = uiState)

            // Tarjeta Informativa de Eficiencia de Batería y Filtro Delta
            EfficiencyInfoCard(uiState = uiState)

            Spacer(modifier = Modifier.weight(1f))

            // Botón Prominente Dinámico para Alternar el Monitoreo
            StartStopTrackingButton(
                isTracking = uiState.isTracking,
                isSensorAvailable = uiState.isSensorAvailable,
                onClick = {
                    if (uiState.isTracking) {
                        viewModel.stopTracking(context)
                    } else {
                        // Verificar permiso de notificación en Android 13+
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED

                            if (!hasPermission) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.startTracking(context)
                            }
                        } else {
                            viewModel.startTracking(context)
                        }
                    }
                }
            )

            // Botón secundario para reiniciar métricas
            if (uiState.sampleCount > 0) {
                OutlinedButton(
                    onClick = { viewModel.resetMetrics() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reiniciar contador"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reiniciar Contador de Muestras")
                }
            }
        }
    }
}

/**
 * Tarjeta estilizada para visualizar en vivo los datos capturados por el sensor.
 */
@Composable
private fun SensorDataCard(
    uiState: RunTrackUiState
) {
    val sensorData = uiState.sensorData

    // Animación de color dinámico para el indicador de estado
    val statusColor by animateColorAsState(
        targetValue = if (uiState.isTracking) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
        animationSpec = tween(durationMillis = 300),
        label = "StatusColorAnimation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Encabezado con estado en tiempo real
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isTracking) "EN MONITOREO" else "DETENIDO",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = statusColor
                    )
                }

                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = "Estado del sensor",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = uiState.sensorStatus,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Barra indicadora de actividad
            if (uiState.isTracking) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = statusColor,
                    trackColor = statusColor.copy(alpha = 0.2f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Sección 1: Componentes del Vector de Rotación (X, Y, Z)
            Text(
                text = "Vector de Rotación de Hardware (Fusion):",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SensorMetricChip(label = "Eje X", value = String.format(Locale.US, "%.4f", sensorData.x))
                SensorMetricChip(label = "Eje Y", value = String.format(Locale.US, "%.4f", sensorData.y))
                SensorMetricChip(label = "Eje Z", value = String.format(Locale.US, "%.4f", sensorData.z))
            }

            // Sección 2: Orientación Espacial Calculada
            Text(
                text = "Orientación Espacial (Ángulos):",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SensorMetricChip(
                    label = "Azimuth (Guiñada)",
                    value = String.format(Locale.US, "%.1f°", sensorData.azimuth)
                )
                SensorMetricChip(
                    label = "Pitch (Inclinación)",
                    value = String.format(Locale.US, "%.1f°", sensorData.pitch)
                )
                SensorMetricChip(
                    label = "Roll (Balanceo)",
                    value = String.format(Locale.US, "%.1f°", sensorData.roll)
                )
            }

            // Indicador gráfico de balanceo (Roll / Inclinación)
            OrientationVisualizer(pitch = sensorData.pitch, roll = sensorData.roll)
        }
    }
}

/**
 * Componente visual que muestra una barra animada basada en la inclinación física del teléfono.
 */
@Composable
private fun OrientationVisualizer(pitch: Float, roll: Float) {
    val animatedRoll by animateFloatAsState(
        targetValue = (roll.coerceIn(-90f, 90f) + 90f) / 180f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "RollAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Inclinación Lateral (Roll):",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = String.format(Locale.US, "Pitch: %.1f° | Roll: %.1f°", pitch, roll),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedRoll },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

/**
 * Chip individual para la visualización formateada de métricas numéricas.
 */
@Composable
private fun SensorMetricChip(
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .width(100.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Tarjeta informativa que detalla el ahorro de energía mediante FIFO Batching y el Filtro Delta.
 */
@Composable
private fun EfficiencyInfoCard(
    uiState: RunTrackUiState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Métricas de Eficiencia Energética",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Muestras Procesadas (Δ > 0.05):",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${uiState.sampleCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "FIFO Latency / Sensor Hub:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "1000 ms (1s Batch)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

/**
 * Botón prominente con colores y texto dinámicos para iniciar o detener el monitoreo.
 */
@Composable
private fun StartStopTrackingButton(
    isTracking: Boolean,
    isSensorAvailable: Boolean,
    onClick: () -> Unit
) {
    // Animación fluida de color entre verde (inicio) y rojo (detener)
    val buttonBackgroundColor by animateColorAsState(
        targetValue = if (!isSensorAvailable) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else if (isTracking) {
            Color(0xFFD32F2F) // Rojo para detener
        } else {
            Color(0xFF2E7D32) // Verde para iniciar
        },
        animationSpec = tween(durationMillis = 350),
        label = "ButtonColorAnimation"
    )

    Button(
        onClick = onClick,
        enabled = isSensorAvailable,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonBackgroundColor,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (!isSensorAvailable) {
                    "Sensor No Disponible"
                } else if (isTracking) {
                    "Detener Carrera"
                } else {
                    "Iniciar Carrera"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
