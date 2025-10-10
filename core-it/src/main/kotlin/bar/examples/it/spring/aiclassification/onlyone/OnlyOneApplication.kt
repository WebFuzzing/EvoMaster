package bar.examples.it.spring.aiclassification.onlyone

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
@RequestMapping(path = ["/OnlyOne4Testing"])
@RestController
open class OnlyOneApplication {

    class ACOnlyOneDto(

        var a : Boolean? = null,

        var b : String? = null,

        var c : Long? = null,

        var d : ACOnlyOneEnum? = null,

        var e : List<String>? = null,

        )

    enum class ACOnlyOneEnum {

        FOO, BAR, HELLO
    }

    @GetMapping
    open fun get(
        @RequestParam("x") x: String?,
        @RequestParam("y") y: Int?,
        @RequestParam("z" )z: Boolean?
    ) : ResponseEntity<String> {

        val px = x != null
        val pz = z == true

        // OnlyOne(x,z=true)
        if(! ( (px&&!pz) || (!px&&pz))){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.ok().body("OK")
    }

    @PostMapping
    open fun post(@RequestBody(required = true) body : ACOnlyOneDto) : ResponseEntity<String> {

        val pa = body.a == false
        val pd = body.d == ACOnlyOneEnum.FOO

        // OnlyOne(a=false,d=FOO)
        if(! ((pa&&!pd) || (!pa&&pd))){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.status(201).body("OK")
    }


}