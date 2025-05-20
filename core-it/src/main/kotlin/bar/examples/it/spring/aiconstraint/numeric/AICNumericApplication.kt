package bar.examples.it.spring.aiconstraint.numeric

import io.swagger.v3.oas.annotations.Parameter
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api"])
@RestController
open class AICNumericApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(AICNumericApplication::class.java, *args)
        }

    }



    @GetMapping("/numeric")
    open fun getString(
        @RequestParam("x", required = true) @Parameter(required=true) x: Int
    ) : ResponseEntity<String> {

        if(x in 381..421){
            return ResponseEntity.status(200).build()
        }

        return ResponseEntity.status(400).build()
    }







}