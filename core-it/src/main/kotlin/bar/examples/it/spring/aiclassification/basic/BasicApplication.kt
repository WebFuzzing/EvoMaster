package bar.examples.it.spring.aiclassification.basic

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.ws.rs.QueryParam

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/basic"])
@RestController
open class BasicApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BasicApplication::class.java, *args)
        }
    }

    @GetMapping
    open fun getData(
        @RequestParam("x") x: String?,
        @RequestParam("y") y: Int?,
        @RequestParam("z") z: Boolean?,
    ): ResponseEntity<String> {

        // No dependency, just constraint on a single variable
        if (y == null) {
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.ok().body("OK")
    }
}