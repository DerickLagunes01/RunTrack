package mx.edu.utez.runtrack.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mx.edu.utez.runtrack.MainActivity
import mx.edu.utez.runtrack.data.RunTrackRepository
import mx.edu.utez.runtrack.sensor.RunTrackSensorManager

/**
 * Servicio en segundo plano (Foreground Service) de alta eficiencia energética para la app "RunTrack".
 *
 * Mantiene activo el registro de sensores mediante hardware FIFO batching incluso cuando la pantalla
 * está apagada o la aplicación se encuentra minimizada.
 */
class RunTrackForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var sensorManager: RunTrackSensorManager? = null

    companion object {
        const val ACTION_START = "mx.edu.utez.runtrack.action.START_TRACKING"
        const val ACTION_STOP = "mx.edu.utez.runtrack.action.STOP_TRACKING"

        private const val CHANNEL_ID = "runtrack_foreground_channel"
        private const val CHANNEL_NAME = "Monitoreo Deportivo RunTrack"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        sensorManager = RunTrackSensorManager(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundTracking()
            ACTION_STOP -> stopForegroundTracking()
            else -> startForegroundTracking()
        }
        return START_STICKY
    }

    private fun startForegroundTracking() {
        // Notificación persistente requerida para el Foreground Service
        val notification = buildNotification("Monitoreando carrera en segundo plano...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }
            startForeground(NOTIFICATION_ID, notification, foregroundServiceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Notificar al repositorio que el servicio inició
        RunTrackRepository.onServiceStart(applicationContext)

        // Escuchar el flujo de datos del sensor
        sensorManager?.let { manager ->
            manager.sensorData
                .onEach { data ->
                    RunTrackRepository.updateSensorReadings(data)
                }
                .launchIn(serviceScope)

            manager.startListening()
        }
    }

    private fun stopForegroundTracking() {
        sensorManager?.stopListening()
        RunTrackRepository.onServiceStop()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación activa mientras se monitorean los datos del sensor de movimiento"
                setShowBadge(false)
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, RunTrackForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RunTrack - Monitoreo Activo")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Detener",
                stopPendingIntent
            )
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.stopListening()
        RunTrackRepository.onServiceStop()
        serviceScope.cancel()
    }
}
