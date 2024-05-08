package bar.examples.it.spring.multipleendpoints

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/endpoint"])
@RestController
open class MultipleEndpointsApplication {


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(MultipleEndpointsApplication::class.java, *args)
        }
    }

    @GetMapping("/endpoint1/setStatus/{status}")
    open fun getByStatusEndpoint1(@PathVariable status: Int) : ResponseEntity<String> {

        return ResponseEntity.status(status).body("endpoint1")
    }

    @GetMapping("/endpoint2/setStatus/{status}")
    open fun getByStatusEndpoint2(@PathVariable status: Int) : ResponseEntity<String> {

        return ResponseEntity.status(status).body("endpoint2")
    }

    @GetMapping("/endpoint3/setStatus/{status}")
    open fun getByStatusEndpoint3(@PathVariable status: Int) : ResponseEntity<String> {

        return ResponseEntity.status(status).body("endpoint3")
    }

    @GetMapping("/endpoint4/setStatus/{status}")
    open fun getByStatusEndpoint4(@PathVariable status: Int) : ResponseEntity<String> {

        return ResponseEntity.status(status).body("endpoint4")
    }

    @GetMapping("/endpoint5/setStatus/{status}")
    open fun getByStatusEndpoint5(@PathVariable status: Int) : ResponseEntity<String> {

        return ResponseEntity.status(status).body("endpoint5")
    }
}