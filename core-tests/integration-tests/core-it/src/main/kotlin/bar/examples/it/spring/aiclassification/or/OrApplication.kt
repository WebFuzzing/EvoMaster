package bar.examples.it.spring.aiclassification.or

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
@RequestMapping(path = ["/Or4Testing"])
@RestController
open class OrApplication {

    class ACOrDto(

        var a : Boolean? = null,

        var b : String? = null,

        var c : Long? = null,

        var d: Long? = null,

        var e : String? = null,

        var f: Boolean? = null,
    )

    @GetMapping
    open fun get(
        @RequestParam("x") x: String?,
        @RequestParam("y") y: Int?,
        @RequestParam("z" )z: Boolean?
    ) : ResponseEntity<String> {

        // x or z=true
        if(! (x!=null || z == true )){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.ok().body("OK")
    }

    @PostMapping
    open fun post(@RequestBody(required = true) body : ACOrDto) : ResponseEntity<String> {

        // a=true or b
        if(! (body.a == true || body.b != null )){
            return ResponseEntity.status(400).build()
        }

        // e or f=false
        if(! (body.f == false || body.e != null )){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.status(201).body("OK")
    }


}