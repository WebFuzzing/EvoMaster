package bar.examples.it.spring.stringvariables

import io.swagger.v3.oas.annotations.Parameter
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/sva"])
@RestController
open class StringVariablesApplication {

    @PostMapping
    open fun post(
        @RequestParam("x", required = false) x: String?,
        @RequestParam("y", required = true) @Parameter(required=true) y: String?,
        @RequestBody body: StringVariablesDto): ResponseEntity<String> {

        if(x == null){
            return ResponseEntity.status(400).build()
        }

        if(x == "hello"){
            return ResponseEntity.status(200).build()
        }
        if(y == "there"){
            return ResponseEntity.status(200).build()
        }

        if(body.foo == "how"){
            return ResponseEntity.status(200).build()
        }

        if(body.nested?.bar == "is it"){
            return ResponseEntity.status(200).build()
        }

        return ResponseEntity.status(400).build()
    }
}