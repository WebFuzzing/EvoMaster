package bar.examples.it.spring.cleanupcreate

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/resources"])
@RestController
open class CleanUpDeleteApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(CleanUpDeleteApplication::class.java, *args)
        }

        private val data = mutableMapOf<Int, String>()

        fun reset(){
            data.clear()
        }

        fun numberExistingData() = data.size
    }


    @PutMapping(path = ["/{id}"])
    open fun put(
        @PathVariable("id") id: Int
    ): ResponseEntity<Any> {

        data[id] = "Data for $id"
        return ResponseEntity.status(200).build()
    }

    @DeleteMapping(path = ["/{id}"])
    open fun delete(@PathVariable("id") id: Int): ResponseEntity<String> {

        if(!data.containsKey(id)){
            return ResponseEntity.status(404).build()
        }

        data.remove(id)

        return ResponseEntity.status(204).build()
    }
}