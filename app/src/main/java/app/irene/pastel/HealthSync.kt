package app.irene.pastel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import kotlin.reflect.KClass

/**
 * Reads the last 7 days from Health Connect (steps, sleep, weight, resting heart rate,
 * blood pressure, water, body fat) and sends one summary per day to Irene OS.
 * Read-only: nothing is ever written back to Health Connect.
 */
object HealthSync {
    const val REQUEST_CODE = 4242
    private const val ENDPOINT = "https://ireneos.app/api/health/sync"
    private const val HC_PACKAGE = "com.google.android.apps.healthdata"
    private const val EVERY_MS = 60 * 60 * 1000L // at most once an hour
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var running = false

    private val TYPES: List<KClass<out Record>> = listOf(
        StepsRecord::class, SleepSessionRecord::class, WeightRecord::class, RestingHeartRateRecord::class,
        BloodPressureRecord::class, HydrationRecord::class, BodyFatRecord::class,
    )
    val PERMISSIONS: Set<String> = TYPES.map { HealthPermission.getReadPermission(it) }.toSet()

    private fun prefs(c: Context) = c.getSharedPreferences("pastel", Context.MODE_PRIVATE)
    fun key(c: Context): String = prefs(c).getString("health_key", "").orEmpty()
    fun setKey(c: Context, k: String) = prefs(c).edit().putString("health_key", k.trim()).putLong("health_last", 0L).apply()
    fun lastSync(c: Context): Long = prefs(c).getLong("health_last", 0L)

    /** 0 = ready, 1 = Health Connect needs installing/updating, 2 = not supported */
    fun availability(c: Context): Int = when (HealthConnectClient.getSdkStatus(c, HC_PACKAGE)) {
        HealthConnectClient.SDK_AVAILABLE -> 0
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> 1
        else -> 2
    }

    fun openStore(a: Activity) {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$HC_PACKAGE&url=healthconnect%3A%2F%2Fonboarding"))
            .setPackage("com.android.vending")
        runCatching { a.startActivity(i) }.onFailure {
            a.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$HC_PACKAGE")))
        }
    }

    /** Shows Health Connect's own permission screen; the answer arrives in onActivityResult(REQUEST_CODE). */
    fun requestPermissions(a: Activity) {
        val intent = PermissionController.createRequestPermissionResultContract().createIntent(a, PERMISSIONS)
        a.startActivityForResult(intent, REQUEST_CODE)
    }

    /** Called from onResume: quietly syncs when it's been an hour. */
    fun maybeSync(c: Context) {
        if (key(c).isBlank() || availability(c) != 0) return
        if (System.currentTimeMillis() - lastSync(c) < EVERY_MS) return
        sync(c) { _, _ -> }
    }

    fun sync(c: Context, done: (ok: Boolean, message: String) -> Unit) {
        val app = c.applicationContext
        val k = key(app)
        if (k.isBlank()) { done(false, "add your health sync key first"); return }
        if (running) return
        running = true
        scope.launch {
            val result = runCatching { syncNow(app, k) }
            running = false
            result.onSuccess { (days, msg) ->
                if (days >= 0) prefs(app).edit().putLong("health_last", System.currentTimeMillis()).apply()
                done(days >= 0, msg)
            }.onFailure { done(false, it.message ?: "sync failed") }
        }
    }

    private suspend fun syncNow(c: Context, key: String): Pair<Int, String> {
        val client = HealthConnectClient.getOrCreate(c)
        val granted = client.permissionController.getGrantedPermissions()
        if (granted.isEmpty()) return -1 to "allow Health Connect access first"
        fun can(t: KClass<out Record>) = HealthPermission.getReadPermission(t) in granted

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val first = today.minusDays(6)
        val days = linkedMapOf<LocalDate, JSONObject>()
        for (i in 0..6) days[first.plusDays(i.toLong())] = JSONObject().put("date", first.plusDays(i.toLong()).toString())
        fun day(t: Instant) = days[t.atZone(zone).toLocalDate()]
        val start = first.atStartOfDay()
        val end = today.plusDays(1).atStartOfDay()
        val instantRange = TimeRangeFilter.between(start.atZone(zone).minusHours(14).toInstant(), end.atZone(zone).toInstant())

        // steps + water: totals per day (Health Connect de-duplicates overlapping sources)
        if (can(StepsRecord::class) || can(HydrationRecord::class)) {
            val metrics = mutableSetOf<AggregateMetric<*>>()
            if (can(StepsRecord::class)) metrics.add(StepsRecord.COUNT_TOTAL)
            if (can(HydrationRecord::class)) metrics.add(HydrationRecord.VOLUME_TOTAL)
            runCatching {
                client.aggregateGroupByPeriod(AggregateGroupByPeriodRequest(metrics, TimeRangeFilter.between(start, end), Period.ofDays(1)))
            }.getOrNull()?.forEach { g ->
                val d = days[g.startTime.toLocalDate()] ?: return@forEach
                g.result[StepsRecord.COUNT_TOTAL]?.let { if (it > 0) d.put("steps", it) }
                g.result[HydrationRecord.VOLUME_TOTAL]?.let { if (it.inFluidOuncesUs > 0) d.put("water_oz", Math.round(it.inFluidOuncesUs)) }
            }
        }
        // sleep: hours of each night, counted on the morning you woke up
        if (can(SleepSessionRecord::class)) {
            read(client, SleepSessionRecord::class, instantRange).forEach { s ->
                val d = day(s.endTime) ?: return@forEach
                val hrs = (s.endTime.toEpochMilli() - s.startTime.toEpochMilli()) / 3_600_000.0
                d.put("sleep_hrs", Math.round((d.optDouble("sleep_hrs", 0.0) + hrs) * 100) / 100.0)
            }
        }
        // latest reading of the day
        if (can(WeightRecord::class)) read(client, WeightRecord::class, instantRange).sortedBy { it.time }
            .forEach { w -> day(w.time)?.put("weight_lb", Math.round(w.weight.inPounds * 10) / 10.0) }
        if (can(RestingHeartRateRecord::class)) read(client, RestingHeartRateRecord::class, instantRange).sortedBy { it.time }
            .forEach { h -> day(h.time)?.put("resting_hr", h.beatsPerMinute) }
        if (can(BloodPressureRecord::class)) read(client, BloodPressureRecord::class, instantRange).sortedBy { it.time }
            .forEach { b -> day(b.time)?.apply { put("bp_sys", Math.round(b.systolic.inMillimetersOfMercury)); put("bp_dia", Math.round(b.diastolic.inMillimetersOfMercury)) } }
        if (can(BodyFatRecord::class)) read(client, BodyFatRecord::class, instantRange).sortedBy { it.time }
            .forEach { f -> day(f.time)?.put("body_fat", Math.round(f.percentage.value * 10) / 10.0) }

        val withData = days.values.filter { it.length() > 1 }
        if (withData.isEmpty()) return 0 to "no health data in the last 7 days yet"
        val body = JSONObject().put("source", "health-connect").put("days", JSONArray(withData))
        val res = post(key, body.toString())
        return withData.size to "synced ${withData.size} day${if (withData.size == 1) "" else "s"} ♡ ($res)"
    }

    private suspend fun <T : Record> read(client: HealthConnectClient, type: KClass<T>, range: TimeRangeFilter): List<T> =
        runCatching { client.readRecords(ReadRecordsRequest(recordType = type, timeRangeFilter = range, pageSize = 1000)).records }.getOrDefault(emptyList())

    private fun post(key: String, json: String): String {
        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000; readTimeout = 20000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $key")
        }
        conn.outputStream.use { it.write(json.toByteArray()) }
        val code = conn.responseCode
        val text = runCatching { (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText() }.getOrNull().orEmpty()
        conn.disconnect()
        if (code == 401) throw IllegalStateException("Irene OS doesn't know that key — make a new one in Settings → Health sync")
        if (code !in 200..299) throw IllegalStateException("Irene OS said $code ${text.take(120)}")
        return runCatching { "${JSONObject(text).optInt("written")} updates" }.getOrDefault("ok")
    }
}
