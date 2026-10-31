package mx.edu.utez.runtrack.data

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.edu.utez.runtrack.sensor.RunSensorData
import mx.edu.utez.runtrack.sensor.RunTrackSensorManager
import mx.edu.utez.runtrack.service.RunTrackForegroundService

/**
 * Repositorio y puente centralizado de datos para coordinar el [RunTrackSensorManager]
 * y el [RunTrackForegroundService] con la capa de presentación (ViewModel).
 */
object RunTrackRepository {

    private var sensorManager: RunTrackSensorManager? = null

    private val _sensorData = MutableStateFlow(RunSensorData())
    val sensorData: StateFlow<RunSensorData> = _sensorData.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _sensorStatus = MutableStateFlow("Listo para iniciar")
    val sensorStatus: StateFlow<String> = _sensorStatus.asStateFlow()

    private val _sampleCount = MutableStateFlow(0L)
    val sampleCount: StateFlow<Long> = _sampleCount.asStateFlow()

    private val _isSensorAvailable = MutableStateFlow(true)
    val isSensorAvailable: StateFlow<Boolean> = _isSensorAvailable.asStateFlow()

    /**
     * Inicializa el gestor de sensores si aún no está instanciado.
     */
    fun initialize(context: Context) {
        if (sensorManager == null) {
            val manager = RunTrackSensorManager(context.applicationContext)
            sensorManager = manager
            _isSensorAvailable.value = manager.isSensorAvailable.value
        }
    }

    /**
     * Inicia el rastreo de movimiento lanzando el Foreground Service para persistencia en segundo plano.
     */
    fun startTracking(context: Context) {
        initialize(context)

        val intent = Intent(context, RunTrackForegroundService::class.java).apply {
            action = RunTrackForegroundService.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    /**
     * Detiene el rastreo y envía la orden de finalización al Foreground Service.
     */
    fun stopTracking(context: Context) {
        val intent = Intent(context, RunTrackForegroundService::class.java).apply {
            action = RunTrackForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }

    /**
     * Inicia la captura física de sensores (llamado por el Foreground Service o directamente).
     */
    internal fun onServiceStart(context: Context) {
        initialize(context)
        val manager = sensorManager ?: return

        val started = manager.startListening()
        if (started) {
            _isTracking.value = true
            _sensorStatus.value = "Monitoreando en segundo plano (50Hz FIFO Batching)"
        } else {
            _isTracking.value = false
            _sensorStatus.value = "Error: Sensor Vector de Rotación no disponible"
        }
    }

    /**
     * Actualiza las lecturas de los sensores recibidas desde el sensor manager.
     */
    internal fun updateSensorReadings(data: RunSensorData) {
        _sensorData.value = data
        _sampleCount.value += 1
    }

    /**
     * Detiene la captura física de sensores y restablece estados.
     */
    internal fun onServiceStop() {
        sensorManager?.stopListening()
        _isTracking.value = false
        _sensorStatus.value = "Monitoreo detenido"
    }

    /**
     * Reinicia el contador de muestras acumuladas.
     */
    fun resetSampleCount() {
        _sampleCount.value = 0L
    }
}
