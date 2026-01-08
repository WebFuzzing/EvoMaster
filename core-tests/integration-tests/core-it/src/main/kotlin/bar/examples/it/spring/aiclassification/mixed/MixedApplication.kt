package bar.examples.it.spring.aiclassification.mixed

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/Mixed4Testing"])
@RestController
open class MixedApplication {

    enum class ACMixedEnum {

        HELLO, X, Y, Z
    }

    @GetMapping
    open fun get(
        @RequestParam("x") x: Int?,
        @RequestParam("y") y: Int?,
        @RequestParam("a" )a: Boolean?,
        @RequestParam("b" )b: Boolean?,
        @RequestParam("s") s: String?,
        @RequestParam("c") c: Boolean?,
        @RequestParam("d") d: ACMixedEnum?,
    ) : ResponseEntity<String> {

        if(x == null || y == null || x > y){
            return ResponseEntity.status(400).build()
        }

        if(a == true && b == true){
            return ResponseEntity.status(400).build()
        }

        if(s==null){
            return ResponseEntity.status(400).build()
        }

        if(c == true){
            if(d != ACMixedEnum.HELLO){
                return ResponseEntity.status(400).build()
            }
        }

        return ResponseEntity.ok().body("OK")
    }

}