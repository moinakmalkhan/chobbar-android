package shop.chobbar.app.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Latin digits everywhere (as in Pakistani retail), localized grouping and month names. */
private fun numberLocale(locale: Locale) = Locale.Builder().setLanguage(locale.language).setRegion("PK").setExtension('u', "nu-latn").build()

fun formatNumber(value: Number, locale: Locale): String = NumberFormat.getIntegerInstance(numberLocale(locale)).format(value)

fun formatMoney(value: Double, locale: Locale, currency: String = "Rs"): String = "$currency ${formatNumber(Math.round(value), locale)}"

fun parseInstant(iso: String?): Instant? = iso?.let {
    runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull()
        ?: runCatching { LocalDate.parse(it.take(10)).atStartOfDay(ZoneId.systemDefault()).toInstant() }.getOrNull()
}

fun formatDate(iso: String?, locale: Locale, style: FormatStyle = FormatStyle.MEDIUM): String {
    val instant = parseInstant(iso) ?: return "—"
    return DateTimeFormatter.ofLocalizedDate(style).withLocale(numberLocale(locale)).withZone(ZoneId.systemDefault()).format(instant)
}

fun formatDateTime(iso: String?, locale: Locale): String {
    val instant = parseInstant(iso) ?: return ""
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(numberLocale(locale)).withZone(ZoneId.systemDefault()).format(instant)
}

@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

object Pk {
    val CITIES = mapOf(
        "Lahore" to "Punjab", "Karachi" to "Sindh", "Islamabad" to "Islamabad Capital Territory", "Rawalpindi" to "Punjab",
        "Faisalabad" to "Punjab", "Multan" to "Punjab", "Sialkot" to "Punjab", "Gujranwala" to "Punjab", "Gujrat" to "Punjab",
        "Sargodha" to "Punjab", "Bahawalpur" to "Punjab", "Sahiwal" to "Punjab", "Sheikhupura" to "Punjab", "Peshawar" to "Khyber Pakhtunkhwa",
        "Mardan" to "Khyber Pakhtunkhwa", "Abbottabad" to "Khyber Pakhtunkhwa", "Hyderabad" to "Sindh", "Sukkur" to "Sindh",
        "Quetta" to "Balochistan", "Gilgit" to "Gilgit-Baltistan", "Muzaffarabad" to "Azad Kashmir", "Mirpur" to "Azad Kashmir",
    )
    val PROVINCES = listOf("Punjab", "Sindh", "Khyber Pakhtunkhwa", "Balochistan", "Islamabad Capital Territory", "Gilgit-Baltistan", "Azad Kashmir")
    fun provinceFor(city: String) = CITIES.entries.firstOrNull { it.key.equals(city.trim(), ignoreCase = true) }?.value
    fun validPhone(v: String): Boolean {
        val d = v.filter { it.isDigit() || it == '+' }
        return Regex("^(\\+92|0092|92|0)?3\\d{9}$").matches(d) || d.count { it.isDigit() } >= 10
    }
    fun validEmail(v: String) = Regex("^\\S+@\\S+\\.\\S+$").matches(v.trim())
}
