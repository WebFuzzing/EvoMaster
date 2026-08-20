package bar.examples.it.spring.dynamicpath

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/dynamicpath"])
@RestController
open class DynamicPathApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(DynamicPathApplication::class.java, *args)
        }
    }


    @PutMapping(path = ["/x/{id}"])
    fun putX(@RequestBody body: String,
             @PathVariable id: String,
             @RequestParam(required = false) foo: String?
    ): ResponseEntity<String> {

        return ResponseEntity.ok().body("OK")
    }

    @GetMapping(path = ["/x/{id}"])
    fun getX(@PathVariable id: String,
             @RequestParam(required = false) bar: Boolean?,
             @RequestParam(required = false) foo: String?,
             @RequestParam(required = true)  k: Boolean
    ): ResponseEntity<String>{

        return ResponseEntity.ok().body("OK")
    }
}