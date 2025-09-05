package bar.examples.it.spring.body

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/body"])
@RestController
open class BodyApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BodyApplication::class.java, *args)
        }
    }


    @PostMapping
    open fun post(@RequestBody body: BodyDto): ResponseEntity<String> {


        if(body.s == "foo"){
            return ResponseEntity.ok("A")
        }

        if(body.d != null && body.d!! < 0){
            return ResponseEntity.ok("B")
        }

        if(body.l != null && body.l!!.size == 2){
            return ResponseEntity.ok("C")
        }

        if(body.o?.s == "bar"){
            return ResponseEntity.ok("D")
        }

        if(body.rb == true && body.ri == 42){
            return ResponseEntity.ok("E")
        }

        return ResponseEntity.status(400).build()
    }
}