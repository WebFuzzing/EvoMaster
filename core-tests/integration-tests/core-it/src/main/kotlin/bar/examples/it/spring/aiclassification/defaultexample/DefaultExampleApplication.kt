package bar.examples.it.spring.aiclassification.defaultexample

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/DefaultExample4Testing"])
@RestController
open class DefaultExampleApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(DefaultExampleApplication::class.java, *args)
        }
    }

    @GetMapping
    open fun getData(
        @RequestParam("x") x: Int?, //examples: 11223344
        @RequestParam("y") y: Int?, //default 42
        @RequestParam("z") z: Int?,
    ): ResponseEntity<String> {


        if(x!=null && y!=null && y < x){
            //if 'x' using example, then hard to pass this constraint
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.ok().body("OK")
    }
}