package bar.examples.it.spring.pathstatus

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/pathstatus"])
@RestController
open class PathStatusApplication {


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(PathStatusApplication::class.java, *args)
        }
    }

    @GetMapping("/byStatus/{status}")
    open fun getByStatus(@PathVariable status: Int) : ResponseEntity<String> {

        return ResponseEntity.status(status).body("byStatus")
    }

    @GetMapping("/others/{x}")
    open fun getOthers(@PathVariable x: Int) : ResponseEntity<String> {

        return ResponseEntity.status(200).body("$x")
    }



}