package bar.examples.it.spring.aiclassification.arithmetic

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/Arithmetic4Testing"])
@RestController
open class ArithmeticApplication {

    class ACArithmeticDto(

        var a : Boolean? = null,

        var b : String? = null,

        var c : Long? = null,

        var d : List<String>? = null,

        var e : Long? = null,

        var f : Float? = null,

        var g : Float? = null,
    )

    @GetMapping
    open fun get(
        @RequestParam("x") x: Int?,
        @RequestParam("y") y: Int?,
        @RequestParam("z" )z: Double?,
        @RequestParam("k" )k: Double?,
        @RequestParam("s") s: String?,
    ) : ResponseEntity<String> {

        if(! (x!! < y!!)){
            return ResponseEntity.status(400).build()
        }

        if(z != null && k != null) {
            if (!(z >= k)) {
                return ResponseEntity.status(400).build()
            }
        }

        return ResponseEntity.ok().body("OK")
    }

    @PostMapping
    open fun post(@RequestBody(required = true) body : ACArithmeticDto) : ResponseEntity<String> {

        val x = body.c!!
        val y = body.e!!
        val z = body.f!!
        val k = body.g!!

        if(x == y || z < k ){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.status(201).body("OK")
    }

}