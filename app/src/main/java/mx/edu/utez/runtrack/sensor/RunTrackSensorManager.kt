package mx.edu.utez.runtrack.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

/**
 * Gestor de sensores de bajo consumo optimizado para el monitoreo deportivo "RunTrack".
 *
 * Características clave de optimización energética:
 * 1. Usa [Sensor.TYPE_ROTATION_VECTOR] que utiliza Sensor Fusion de hardware (acelerómetro, giroscopio y magnetómetro).
 * 2. Implementa **FIFO Batching** con `maxReportLatencyUs = 1_000_000` (1 segundo) y `samplingPeriodUs = 20_000` (50 Hz).
 *    Esto delega el procesamiento continuo al Sensor Hub de hardware y reduce las interrupciones del procesador principal (CPU).
 * 3. Aplica un filtro de variación mínima ($\Delta > 0.05f$) para descartar ruido e inactividad antes de propagar cambios a la UI.
 *
 * @param context Contexto de la aplicación para obtener el servicio de sensores.
 */
class RunTrackSensorManager(context: Context) : SensorEventListener {

    private val sensorManager: SensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationVectorSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    // Estado observable que emite la última lectura procesada
    private val _sensorData = MutableStateFlow(RunSensorData())
    val sensorData: StateFlow<RunSensorData> = _sensorData.asStateFlow()

    // Estado observable de disponibilidad del sensor
    private val _isSensorAvailable = MutableStateFlow(rotationVectorSensor != null)
    val isSensorAvailable: StateFlow<Boolean> = _isSensorAvailable.asStateFlow()

    // Estado observable de si el sensor está escuchando activamente
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    // Últimos valores registrados para el filtro de variación mínima (Delta)
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var isFirstReading = true

    // Matrices temporales para calcular la orientación (evitan alojar objetos en cada evento)
    private val rotationMatrix = FloatArray(9)
    private val orientationValues = FloatArray(3)

    companion object {
        /** Tasa de muestreo de 50 Hz (20,000 microsegundos por muestra) */
        private const val SAMPLING_PERIOD_US = 20_000

        /** Latencia máxima de reporte de 1 segundo (1,000,000 microsegundos) para FIFO Batching de hardware */
        private const val MAX_REPORT_LATENCY_US = 1_000_000

        /** Umbral de variación mínima ($\Delta > 0.05f$) para descartar ruido o lecturas estáticas */
        private const val MIN_DELTA_THRESHOLD = 0.05f
    }

    /**
     * Inicia el monitoreo del sensor de vector de rotación configurando FIFO Batching en el Sensor Hub.
     * @return `true` si la suscripción se realizó correctamente, `false` si el sensor no está disponible.
     */
    fun startListening(): Boolean {
        if (rotationVectorSensor == null) {
            _isSensorAvailable.value = false
            return false
        }

        if (_isListening.value) {
            return true
        }

        // Reiniciar variables del filtro Delta
        isFirstReading = true

        // Registrar listener solicitando FIFO Batching explícito de 1 segundo
        val registered = sensorManager.registerListener(
            this,
            rotationVectorSensor,
            SAMPLING_PERIOD_US,
            MAX_REPORT_LATENCY_US
        )

        _isListening.value = registered
        return registered
    }

    /**
     * Detiene la escucha del sensor para liberar recursos y evitar consumo de energía en estado inactivo.
     */
    fun stopListening() {
        if (_isListening.value) {
            sensorManager.unregisterListener(this)
            _isListening.value = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        val currentX = event.values[0]
        val currentY = event.values[1]
        val currentZ = event.values[2]

        // 1. Filtrado por Variación Mínima (Delta Filter: Delta > 0.05f)
        if (!isFirstReading) {
            val deltaX = currentX - lastX
            val deltaY = currentY - lastY
            val deltaZ = currentZ - lastZ
            val deltaDistance = sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()).toFloat()

            // Descartar la lectura si el movimiento no supera el umbral predeterminado
            if (deltaDistance <= MIN_DELTA_THRESHOLD) {
                return
            }
        } else {
            isFirstReading = false
        }

        // Actualizar últimas lecturas válidas
        lastX = currentX
        lastY = currentY
        lastZ = currentZ

        // 2. Calcular matriz de rotación e inclinaciones espaciales (Azimuth, Pitch, Roll)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationValues)

        // Convertir ángulos de radianes a grados
        val azimuthDeg = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
        val pitchDeg = Math.toDegrees(orientationValues[1].toDouble()).toFloat()
        val rollDeg = Math.toDegrees(orientationValues[2].toDouble()).toFloat()

        // 3. Emitir nuevos datos hacia los consumidores
        _sensorData.value = RunSensorData(
            x = currentX,
            y = currentY,
            z = currentZ,
            azimuth = azimuthDeg,
            pitch = pitchDeg,
            roll = rollDeg,
            timestamp = event.timestamp
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No se requiere procesamiento especial para cambios de precisión en esta implementación
    }
}
