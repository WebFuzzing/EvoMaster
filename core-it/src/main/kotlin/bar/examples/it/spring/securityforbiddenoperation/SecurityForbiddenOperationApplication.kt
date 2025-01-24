package bar.examples.it.spring.securityforbiddenoperation

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.concurrent.ConcurrentHashMap


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/resources"])
@RestController
open class SecurityForbiddenOperationApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(SecurityForbiddenOperationApplication::class.java, *args)
        }

        var disabledCheckDelete = false
        var disabledCheckPut = false
        var disabledCheckPatch = false

        private val data = ConcurrentHashMap<Int,String>()
        private var counter = 0

        fun cleanState(){
            counter = 0
            data.clear()
        }

        fun reset(){
            disabledCheckDelete = false
            disabledCheckPut = false
            disabledCheckPatch = false
            cleanState()
        }
    }




    @PostMapping
    open fun create(
        @RequestHeader("Authorization") auth: String?,
    ) : ResponseEntity<Any> {

        if(!checkAuth(auth)) {
            return ResponseEntity.status(401).build()
        }

        data[counter] = auth!!
        val res = ResponseEntity.created(URI.create("/api/resources/${counter}")).build<Any>()
        counter++
        return res
    }

    private fun checkAuth(auth: String?) = auth != null && (auth == "FOO" || auth == "BAR")


    @DeleteMapping(path = ["/{id}"])
    open fun delete(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable("id") id: Int,
    ): ResponseEntity<Any> {

        if(!checkAuth(auth)) {
            return ResponseEntity.status(401).build()
        }

        if(!data.containsKey(id)){
            return ResponseEntity.status(404).build()
        }

        val source = data.getValue(id)
        if(!disabledCheckDelete && source != auth){
            return ResponseEntity.status(403).build()
        }
        data.remove(id)
        return ResponseEntity.status(204).build()
    }

    @PutMapping(path = ["/{id}"])
    open fun put(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable("id") id: Int,
    ): ResponseEntity<Any> {

        if(!checkAuth(auth)) {
            return ResponseEntity.status(401).build()
        }

        if(!data.containsKey(id)){
            data[id] = auth!!
            return ResponseEntity.status(201).build()
        }

        val source = data.getValue(id)
        if(!disabledCheckPut && source != auth){
            return ResponseEntity.status(403).build()
        }
        return ResponseEntity.status(204).build()
    }

    @PatchMapping(path = ["/{id}"])
    open fun patch(
        @RequestHeader("Authorization") auth: String?,
        @PathVariable("id") id: Int,
    ): ResponseEntity<Any> {

        if(!checkAuth(auth)) {
            return ResponseEntity.status(401).build()
        }

        if(!data.containsKey(id)){
            return ResponseEntity.status(404).build()
        }

        val source = data.getValue(id)
        if(!disabledCheckPatch && source != auth){
            return ResponseEntity.status(403).build()
        }
        return ResponseEntity.status(204).build()
    }


}