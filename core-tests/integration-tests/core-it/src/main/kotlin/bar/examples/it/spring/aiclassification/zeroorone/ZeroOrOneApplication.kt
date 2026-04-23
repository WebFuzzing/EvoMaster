package bar.examples.it.spring.aiclassification.zeroorone

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
@RequestMapping(path = ["/ZeroOrOne4Testing"])
@RestController
open class ZeroOrOneApplication {

    class ACZeroOrOneDto(

        var a : Boolean? = null,

        var b : String? = null,

        var c : Long? = null,

        var d : ACZeroOrOneEnum? = null,

        var e : List<String>? = null,

        var f : ACZeroOrOneEnum? = null,
    )

    enum class ACZeroOrOneEnum {

        HELLO, X
    }

    @GetMapping
    open fun get(
        @RequestParam("x") x: String?,
        @RequestParam("y") y: Int?,
        @RequestParam("z" )z: Boolean?
    ) : ResponseEntity<String> {

        val px = x != null
        val pz = z == true

        // ZeroOrOne(x,z=true)
        if(! ( (px&&!pz) || (!px&&pz) || (!px&&!pz) )){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.ok().body("OK")
    }

    @PostMapping
    open fun post(@RequestBody(required = true) body : ACZeroOrOneDto) : ResponseEntity<String> {

        val pd = body.d == ACZeroOrOneEnum.HELLO
        val pf = body.f == ACZeroOrOneEnum.X

        // ZeroOrOne(d=HELLO,f=X) -> same as both !&& when just two variables
        if( pd && pf){
            return ResponseEntity.status(400).build()
        }

        if(body.a != true && body.b!=null){
            return ResponseEntity.status(400).build()
        }

        return ResponseEntity.status(201).body("OK")
    }

}