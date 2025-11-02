package bar.examples.it.spring.nonworkingdelete

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/resources"])
@RestController
open class NonWorkingDeleteApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(NonWorkingDeleteApplication::class.java, *args)
        }

        private val data = mutableMapOf<Int, String>()

        fun reset(){
            data.clear()
        }
    }

    private fun checkAuth(auth: String?) = auth != null && (auth == "FOO" || auth == "BAR")


    @PutMapping(path = ["/{id}"])
    open fun put(
        @PathVariable("id") id: Int
    ): ResponseEntity<Any> {

        data[id] = "Data for $id"
        return ResponseEntity.status(200).build()
    }

    @GetMapping(path = ["/{id}"])
    open fun get(@PathVariable("id") id: Int): ResponseEntity<String> {

        if(!data.containsKey(id)){
            return ResponseEntity.status(404).build()
        }

        return ResponseEntity.status(200).body(data[id])
    }

    @DeleteMapping(path = ["/{id}"])
    open fun delete(@PathVariable("id") id: Int): ResponseEntity<String> {

        if(!data.containsKey(id)){
            return ResponseEntity.status(404).build()
        }

        //wrong: do nothing
        return ResponseEntity.status(204).build()
    }
}