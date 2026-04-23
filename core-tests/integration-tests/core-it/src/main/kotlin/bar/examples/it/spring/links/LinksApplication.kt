package bar.examples.it.spring.links

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api"])
@RestController
open class LinksApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(LinksApplication::class.java, *args)
        }

        const val secretId = "abc123"
        const val secretCode = "mysecretcode"
    }



    @PostMapping("/auth/{code}")
    open fun postAuth(@PathVariable code: String) : ResponseEntity<LinksDto> {

        if(code != secretCode){
            return ResponseEntity.status(400).build()
        }

        val dto = LinksDto(data = LinksDataDto(secretId, "Foo"))

        return ResponseEntity.status(200).body(dto)
    }


    @GetMapping("/users/{id}")
    open fun getUser(@PathVariable id: String) : ResponseEntity<String> {

        if(id != secretId){
            return ResponseEntity.status(404).build()
        }

        return ResponseEntity.status(200).body("OK")
    }
}