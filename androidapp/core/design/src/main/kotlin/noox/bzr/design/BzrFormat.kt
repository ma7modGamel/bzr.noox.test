package noox.bzr.design

import java.util.Locale
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * The single number formatter shared by spec with iOS (`BzrFormat.swift`), 43 §2:
 * Western Arabic digits everywhere, whatever the device locale.
 */
object BzrFormat {
    /** 350 → "350", 4.9 → "4.9". */
    fun number(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.ROOT, "%.1f", value)

    fun number(value: Int): String = value.toString()

    fun amount(value: Double, currency: String): String = "${number(value)} $currency"

    /** Fills `{name}` placeholders of a template from strings.ar.json. */
    fun fill(template: String, vararg values: Pair<String, String>): String =
        values.fold(template) { text, (key, value) -> text.replace("{$key}", value) }

    fun fill(template: String, n: Int): String = fill(template, "n" to number(n))

    fun fill(template: String, n: Double): String = fill(template, "n" to number(n))

    /** 760 → "12:40". */
    fun countdown(seconds: Int): String =
        String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)

    fun time(instant: Instant, zoneId: ZoneId, morning: String, evening: String): String {
        val time = instant.atZone(zoneId)
        val hour = time.hour
        val twelveHour = if (hour % 12 == 0) 12 else hour % 12
        val period = if (hour < 12) morning else evening
        return String.format(Locale.ROOT, "%d:%02d %s", twelveHour, time.minute, period)
    }

    fun dateLabel(
        instant: Instant,
        relativeTo: Instant,
        zoneId: ZoneId,
        today: String,
        tomorrow: String,
        weekdays: List<String>,
        months: List<String>,
    ): String {
        val date = instant.atZone(zoneId).toLocalDate()
        val reference = relativeTo.atZone(zoneId).toLocalDate()
        return when (ChronoUnit.DAYS.between(reference, date)) {
            0L -> today
            1L -> tomorrow
            else -> "${weekdays[date.dayOfWeek.value % 7]} ${date.dayOfMonth} ${months[date.monthValue - 1]}"
        }
    }
}
