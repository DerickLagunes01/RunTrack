package mx.edu.utez.runtrack.sensor

/**
 * Modelo de datos inmutable que representa una lectura filtrada del vector de rotación
 * del hardware y sus correspondientes ángulos de orientación espacial.
 *
 * @property x Componente X del vector de rotación de hardware (x * sin(theta/2)).
 * @property y Componente Y del vector de rotación de hardware (y * sin(theta/2)).
 * @property z Componente Z del vector de rotación de hardware (z * sin(theta/2)).
 * @property azimuth Ángulo de orientación respecto al Norte magnético (Guiñada / Yaw) en grados [-180°, 180°].
 * @property pitch Ángulo de inclinación frontal (Cabeceo / Pitch) en grados [-90°, 90°].
 * @property roll Ángulo de inclinación lateral (Balanceo / Roll) en grados [-180°, 180°].
 * @property timestamp Estampa de tiempo del evento suministrada por el sensor en nanosegundos o milisegundos.
 */
data class RunSensorData(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val azimuth: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)
