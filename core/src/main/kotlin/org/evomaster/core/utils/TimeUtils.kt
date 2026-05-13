package org.evomaster.core.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object TimeUtils {

    val log: Logger = LoggerFactory.getLogger(TimeUtils::class.java)

    /**
     * Invoke the [function] lambda, which will return some result of generic type [T].
     * Once this is completed, the [loggingFunction] will be automatically called with,
     * as input, the execution time expressed in milliseconds, as well as the [function]'s result
     * of type [T].
     *
     * From https://proandroiddev.com/measuring-execution-times-in-kotlin-460a0285e5ea
     */
    inline fun <T> measureTimeMillis(
        loggingFunction: (Long, T) -> Unit,
        function: () -> T
    ): T {

        val startTime = System.currentTimeMillis()
        val result: T = function.invoke()
        loggingFunction.invoke(System.currentTimeMillis() - startTime, result)

        return result
    }

    fun getElapsedTime(totalInSeconds: Long) : String{

        val seconds = totalInSeconds
        val minutes = seconds / 60.0
        val hours = minutes / 60.0

        val ps = "%d".format(seconds % 60)
        val pm = "%d".format(minutes.toInt() % 60)
        val ph = "%d".format(hours.toInt())

        return "${ph}h ${pm}m ${ps}s"
    }

    /**
     * Given the content of a HTTP Retry-After, return number of seconds to wait.
     * If there is any issue, return a negative number.
     *
     * https://httpwg.org/specs/rfc9110.html#field.retry-after
     * https://httpwg.org/specs/rfc9110.html#http.date
     */
    fun getTimeToWaitInSeconds(retryAfter: String) : Long {

        try {
            return retryAfter.toLong()
        } catch (e: NumberFormatException) {

            val parsed = try{
                ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME)
            } catch (e: DateTimeParseException) {
                log.warn("Failed to parse Retry-After: $retryAfter. Error message: ${e.message}")
                return  -1
            }

            val target = parsed.toInstant()
            val now = Instant.now()

            return Duration.between(now, target).seconds
        }
    }
}
