package mx.checklist.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val utcFormatNoMs = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val localFormat = SimpleDateFormat("dd/MMM/yyyy HH:mm", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }

    private val localFormatShort = SimpleDateFormat("HH:mm 'hs' dd/MM", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }

    fun isoUtcToLocalDateTime(iso: String): String {
        return try {
            val isoClean = iso.replace("Z", "")

            val date = try {
                utcFormat.parse(isoClean)
            } catch (e: Exception) {
                utcFormatNoMs.parse(isoClean)
            }

            date?.let { localFormat.format(it) } ?: iso
        } catch (e: Exception) {
            iso
        }
    }

    fun isoUtcToLocalDateTimeShort(iso: String): String {
        return try {
            val isoClean = iso.replace("Z", "")

            val date = try {
                utcFormat.parse(isoClean)
            } catch (e: Exception) {
                utcFormatNoMs.parse(isoClean)
            }

            date?.let { localFormatShort.format(it) } ?: iso
        } catch (e: Exception) {
            iso
        }
    }

    fun isToday(iso: String): Boolean {
        return try {
            val calendar = Calendar.getInstance()
            calendar.time = utcFormat.parse(iso.replace("Z", "")) ?: return false

            val today = Calendar.getInstance()

            calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        } catch (e: Exception) {
            false
        }
    }
}

