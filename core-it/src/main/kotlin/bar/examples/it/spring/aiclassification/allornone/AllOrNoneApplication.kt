package bar.examples.it.spring.aiclassification.allornone

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/AllOrNone4Testing"])
@RestController
open class AllOrNoneApplication {

    enum class ACAllOrNoneEnum {

        FOO, BAR, HELLO, X, Y, A0, A1, A2, A3
    }

    class ACAllOrNoneDto(

        var a : Boolean? = null,

        var b : String? = null,

        var c : Double? = null,

        var d : ACAllOrNoneEnum? = null,

        // TODO: Handling Lists that provides ArrayGenes is challenging because their length is flexible.
        //       Each ArrayGene can expand into a variable number of elements, which makes
        //       it difficult to map them into a fixed-length input vector for classification.
        //       A proper strategy (e.g., padding, truncation, or embedding) will be needed later
        //       to address this challenge.

//        var e : List<String>? = null,

        )

    @GetMapping
    open fun get(
        @RequestParam("x") x: String?,
        @RequestParam("y") y: Int?,
        @RequestParam("z" )z: Boolean?
    ) : ResponseEntity<String> {

        val px = x != null
        val pz = z == true

        // AllOrNone(x,z=true)
        if(! ( (px&&pz) || (!px&&!pz))){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.ok().body("OK")
    }

    @PostMapping
    open fun post(@RequestBody(required = true) body : ACAllOrNoneDto) : ResponseEntity<String> {

        val pa = body.a == false
        val pd = body.d == ACAllOrNoneEnum.HELLO

        // AllOrNone(a=false,d=HELLO)
        if(! ((pa&&pd) || (!pa&&!pd))){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.status(200).body("OK")
    }


}