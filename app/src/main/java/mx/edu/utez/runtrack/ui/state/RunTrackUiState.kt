package mx.edu.utez.runtrack.ui.state

import mx.edu.utez.runtrack.sensor.RunSensorData

/**
 * Estado inmutable de la interfaz de usuario para la pantalla principal de "RunTrack".
 *
 * @property isTracking Indica si el monitoreo de movimiento y el Foreground Service están activos.
 * @property sensorData Objeto con las métricas actuales del sensor (X, Y, Z, Azimuth, Pitch, Roll).
 * @property sensorStatus Mensaje informativo del estado del sensor o del servicio.
 * @property sampleCount Número acumulado de muestras recibidas que superaron el filtro delta ($\Delta > 0.05f$).
 * @property isSensorAvailable Indica si el dispositivo cuenta con el sensor de hardware necesario.
 */
data class RunTrackUiState(
    val isTracking: Boolean = false,
    val sensorData: RunSensorData = RunSensorData(),
    val sensorStatus: String = "Listo para iniciar carrera",
    val sampleCount: Long = 0L,
    val isSensorAvailable: Boolean = true
)
