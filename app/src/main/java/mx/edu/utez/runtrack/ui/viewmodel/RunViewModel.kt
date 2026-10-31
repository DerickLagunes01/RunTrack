package mx.edu.utez.runtrack.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import mx.edu.utez.runtrack.data.RunTrackRepository
import mx.edu.utez.runtrack.ui.state.RunTrackUiState

/**
 * ViewModel que administra el estado de la UI y coordina las acciones del usuario con
 * el [RunTrackRepository] siguiendo la arquitectura MVVM recomendada en Android.
 */
class RunViewModel : ViewModel() {

    private val repository = RunTrackRepository

    /**
     * Estado reactivo expuesto a la vista como un [StateFlow] inmutable.
     * Combina los flujos individuales del repositorio en un único objeto [RunTrackUiState].
     */
    val uiState: StateFlow<RunTrackUiState> = combine(
        repository.isTracking,
        repository.sensorData,
        repository.sensorStatus,
        repository.sampleCount,
        repository.isSensorAvailable
    ) { isTracking, sensorData, sensorStatus, sampleCount, isSensorAvailable ->
        RunTrackUiState(
            isTracking = isTracking,
            sensorData = sensorData,
            sensorStatus = sensorStatus,
            sampleCount = sampleCount,
            isSensorAvailable = isSensorAvailable
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RunTrackUiState()
    )

    /**
     * Alterna reactivamente el estado de monitoreo de la carrera (Iniciar / Detener).
     *
     * @param context Contexto requerido para la comunicación con el servicio en segundo plano.
     */
    fun toggleTracking(context: Context) {
        if (uiState.value.isTracking) {
            stopTracking(context)
        } else {
            startTracking(context)
        }
    }

    /**
     * Inicia la captura de datos y el Foreground Service.
     */
    fun startTracking(context: Context) {
        repository.startTracking(context)
    }

    /**
     * Detiene la captura de datos y el Foreground Service.
     */
    fun stopTracking(context: Context) {
        repository.stopTracking(context)
    }

    /**
     * Reinicia el contador de muestras.
     */
    fun resetMetrics() {
        repository.resetSampleCount()
    }
}
