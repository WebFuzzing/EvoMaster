package org.evomaster.core.utils

object TimeUtils {


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
}
