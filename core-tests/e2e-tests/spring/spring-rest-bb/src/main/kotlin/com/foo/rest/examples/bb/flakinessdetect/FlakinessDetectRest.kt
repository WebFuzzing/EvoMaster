package com.foo.rest.examples.bb.flakinessdetect

import org.evomaster.e2etests.utils.CoveredTargets
import org.h2.util.MathUtils.randomInt
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

@RestController
@RequestMapping(path = ["/api/flakinessdetect"])
class FlakinessDetectRest {

    companion object{
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS yyyy-MM-dd EEEE 'Week' ww")
    }

    @GetMapping(path = ["/stringfirst/{n}"])
    open fun getFirst( @PathVariable("n") n: Int) : ResponseEntity<String> {
        CoveredTargets.cover("First")
        return ResponseEntity.ok(getPartialDate(n))
    }

    @GetMapping(path = ["/next/{n}"])
    open fun getNext( @PathVariable("n") n: Int) : ResponseEntity<FlakinessDetectData> {
        CoveredTargets.cover("Next")
        return ResponseEntity.ok(FlakinessDetectData(getPartialDate(n), randomInt(n)))
    }

    @GetMapping(path = ["/multiplelines/{num}"])
    open fun getMultipleLines( @PathVariable("num") num: Int) : ResponseEntity<FlakinessDetectData> {

        val num = min(20, max(2, randomInt(num)))

        val msg = (1..num).joinToString(System.lineSeparator()) { "LINE $it" }
        CoveredTargets.cover("MultipleLines")
        return ResponseEntity.ok(FlakinessDetectData(msg, num))
    }

    @GetMapping("/price/estimate")
    fun estimatePrice(@RequestParam base: Int): Map<String, Int> {
        val randomJitter = randomInt(1000)

        val total = base + randomJitter

        CoveredTargets.cover("estimate")
        return mapOf(
            "base" to base,
            "jitter" to randomJitter,
            "total" to total
        )
    }

    @GetMapping("/calculate/timeago")
    fun getTimeAgo(
        @RequestParam(defaultValue = "0") day: Long,
        @RequestParam(defaultValue = "0") hour: Long,
        @RequestParam(defaultValue = "0") min: Long,
        @RequestParam(defaultValue = "0") sec: Long
    ): ResponseEntity<TimeAgoData> {

        CoveredTargets.cover("TimeAgo")
        val msg = listOf(day to "day", hour to "hour", min to "minute", sec to "second")
            .filter { it.first > 0 }
            .joinToString(", ") { (v, u) -> "$v $u${if (v > 1) "s" else ""}" }.takeIf { it.isNotEmpty() }
            ?.let { "$it ago" }
            ?: "just now"

        val pastDate = LocalDateTime.now()
            .minusDays(day).minusHours(hour).minusMinutes(min).minusSeconds(sec)

        return ResponseEntity.ok(TimeAgoData(msg, pastDate.toString()))
    }

    private fun getPartialDate(n: Int) : String {
        val now = LocalDateTime.now().format(formatter)
        val size = max(12, min(now.toString().length, n))

        return now.substring(0, size)
    }
}

data class FlakinessDetectData(
    val first : String, val next : Int)

data class TimeAgoData(
    val message: String,
    val calculatedPastTime: String
)