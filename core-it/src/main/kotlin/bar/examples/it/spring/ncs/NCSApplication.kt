package bar.examples.it.spring.ncs

import bar.examples.it.spring.ncs.imp.Bessj
import bar.examples.it.spring.ncs.imp.Dto
import bar.examples.it.spring.ncs.imp.Expint
import bar.examples.it.spring.ncs.imp.Fisher
import bar.examples.it.spring.ncs.imp.Gammq
import bar.examples.it.spring.ncs.imp.Remainder
import bar.examples.it.spring.ncs.imp.TriangleClassification
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RestController
@RequestMapping(path = ["/api"])
open class NCSApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(NCSApplication::class.java, *args)
        }
    }

    @GetMapping(
        value = ["/triangle/{a}/{b}/{c}"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    open fun checkTriangle(
        @PathVariable("a")
        @Parameter(description = "First edge", required = true)
        a: Int,

        @PathVariable("b")
        @Parameter(description = "Second edge", required = true)
        b: Int,

        @PathVariable("c")
        @Parameter(description = "Third edge", required = true)
        c: Int
    ): ResponseEntity<Dto> {

        val dto = Dto()
        dto.resultAsInt = TriangleClassification.classify(a, b, c)
        return ResponseEntity.ok(dto)
    }

    @GetMapping(
        value = ["/bessj/{n}/{x}"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    open fun bessj(
        @PathVariable("n")
        @Parameter(description = "Order n (2 < n ≤ 1000)", required = true)
        n: Int,

        @PathVariable("x")
        @Parameter(description = "Argument x", required = true)
        x: Double
    ): ResponseEntity<Dto> {
        if (n <= 2 || n > 1000) {
            return ResponseEntity.status(400).build()
        }

        val dto = Dto()
        val bessj = Bessj()
        dto.resultAsDouble = bessj.bessj(n, x)
        return ResponseEntity.ok(dto)
    }

    @GetMapping(
        value = ["/expint/{n}/{x}"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    open fun expint(
        @PathVariable("n")
        @Parameter(description = "Order n", required = true)
        n: Int,

        @PathVariable("x")
        @Parameter(description = "Argument x", required = true)
        x: Double
    ): ResponseEntity<Dto> =
        try {
            val dto = Dto()
            dto.resultAsDouble = Expint.exe(n, x)
            ResponseEntity.ok(dto)
        } catch (e: RuntimeException) {
            ResponseEntity.status(400).build()
        }

    @GetMapping(
        value = ["/fisher/{m}/{n}/{x}"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    open fun fisher(
        @PathVariable("m")
        @Parameter(description = "Degrees of freedom m (≤ 1000)", required = true)
        m: Int,

        @PathVariable("n")
        @Parameter(description = "Degrees of freedom n (≤ 1000)", required = true)
        n: Int,

        @PathVariable("x")
        @Parameter(description = "Argument x", required = true)
        x: Double
    ): ResponseEntity<Dto> {
        if (m > 1000 || n > 1000) {
            return ResponseEntity.status(400).build()
        }

        return try {
            val dto = Dto()
            dto.resultAsDouble = Fisher.exe(m, n, x)
            ResponseEntity.ok(dto)
        } catch (e: RuntimeException) {
            ResponseEntity.status(400).build()
        }
    }

    @GetMapping(
        value = ["/gammq/{a}/{x}"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    open fun gammq(
        @PathVariable("a")
        @Parameter(description = "Shape a", required = true)
        a: Double,

        @PathVariable("x")
        @Parameter(description = "Argument x", required = true)
        x: Double
    ): ResponseEntity<Dto> =
        try {
            val dto = Dto()
            val gammq = Gammq()
            dto.resultAsDouble = gammq.exe(a, x)
            ResponseEntity.ok(dto)
        } catch (e: RuntimeException) {
            ResponseEntity.status(400).build()
        }

    @GetMapping(
        value = ["/remainder/{a}/{b}"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    open fun remainder(
        @PathVariable("a")
        @Parameter(description = "Dividend a (|a| ≤ 10000)", required = true)
        a: Int,

        @PathVariable("b")
        @Parameter(description = "Divisor b (|b| ≤ 10000)", required = true)
        b: Int
    ): ResponseEntity<Dto> {
        val lim = 10_000
        if (a > lim || a < -lim || b > lim || b < -lim) {
            return ResponseEntity.status(400).build()
        }

        val dto = Dto()
        dto.resultAsInt = Remainder.exe(a, b)
        return ResponseEntity.ok(dto)
    }
}