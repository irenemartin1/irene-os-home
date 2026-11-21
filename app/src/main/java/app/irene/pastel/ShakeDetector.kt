package app.irene.pastel

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.sqrt

/** Fires onTripleShake when the phone is shaken 3 times within ~1.5 seconds. */
class ShakeDetector(private val onTripleShake: () -> Unit) : SensorEventListener {

    companion object {
        const val THRESHOLD_G = 2.2f     // how hard a shake has to be (lower = easier)
        const val MIN_GAP_MS = 220L      // ignore jitter within one shake
        const val WINDOW_MS = 1500L      // all 3 shakes must happen within this
        const val COOLDOWN_MS = 1500L    // don't flip back right away
    }

    private var count = 0
    private var firstAt = 0L
    private var lastAt = 0L
    private var cooldownUntil = 0L

    override fun onSensorChanged(e: SensorEvent) {
        val (x, y, z) = Triple(e.values[0], e.values[1], e.values[2])
        val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        if (g < THRESHOLD_G) return
        val now = SystemClock.elapsedRealtime()
        if (now < cooldownUntil || now - lastAt < MIN_GAP_MS) return
        if (count == 0 || now - firstAt > WINDOW_MS) { count = 0; firstAt = now }
        lastAt = now
        count++
        if (count >= 3) {
            count = 0
            cooldownUntil = now + COOLDOWN_MS
            onTripleShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
