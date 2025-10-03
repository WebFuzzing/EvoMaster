package bar.examples.it.spring.aiclassification.imply

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/Imply4Testing"])
@RestController
open class ImplyApplication {

    class ACImplyDto(

        var a : Boolean? = null,

        var b : String? = null,

        var c : Long? = null,

        var d : ACImplyEnum? = null,

        var e : List<String>? = null,

        var f : ACImplyEnum? = null,
    )

    enum class ACImplyEnum {

        HELLO, X
    }

    @GetMapping
    open fun get(
        @RequestParam("x") x: String?,
        @RequestParam("y") y: Int?,
        @RequestParam("z" )z: Boolean?
    ) : ResponseEntity<String> {

        if(z == true){
            if(x == null) {
                return ResponseEntity.status(400).build()
            }
        }

        return ResponseEntity.ok().body("OK")
    }

    @PostMapping
    open fun post(@RequestBody(required = true) body : ACImplyDto) : ResponseEntity<String> {

        if(body.a == true){
            if(! (body.d == ACImplyEnum.HELLO || body.f == ACImplyEnum.HELLO)){
                return ResponseEntity.status(400).build()
            }
        }

        return ResponseEntity.status(201).body("OK")
    }


}