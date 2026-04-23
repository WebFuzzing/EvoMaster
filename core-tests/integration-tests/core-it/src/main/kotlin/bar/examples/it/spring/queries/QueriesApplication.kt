package bar.examples.it.spring.queries

import io.swagger.v3.oas.annotations.Parameter
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api"])
@RestController
open class QueriesApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(QueriesApplication::class.java, *args)
        }

    }



    @GetMapping("/queries/string")
    open fun getString(
        @RequestParam("x", required = false) x: String?,
        @RequestParam("y", required = true) @Parameter(required=true) y: String?,
    ) : ResponseEntity<String> {

        if(x == "FOO" && y == "BAR") {
            return ResponseEntity.status(200).body("OK")
        }

        return ResponseEntity.status(400).body("WRONG")
    }

    @GetMapping("/queries/numbers")
    open fun getNumbers(
        @RequestParam("a", required = false) a: Int?,
        @RequestParam("b", required = true) @Parameter(required=true)  b: Double?,
        @RequestParam("c", required = true) @Parameter(required=true)  c: Boolean?,
    ) : ResponseEntity<String> {

        if(a==42 && b!!<0 && c!!){
            return ResponseEntity.status(200).body("OK")
        }

        return ResponseEntity.status(400).body("WRONG")
    }






}