package bar.examples.it.spring.cleanupuuid

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/resources"])
@RestController
open class CleanUpUUIDApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(CleanUpUUIDApplication::class.java, *args)
        }

        private val data = mutableMapOf<UUID, String>()

        fun reset(){
            data.clear()
        }

        fun numberExistingData() = data.size
    }


    @PostMapping(path = [""])
    open fun post(): ResponseEntity<CleanUpUUIDDto> {
        val id = UUID.randomUUID()
        data[id] = "Data for $id"
        return ResponseEntity.status(201).body(CleanUpUUIDDto(id))
    }


    @PutMapping(path = ["/{id}"])
    open fun put(
        @PathVariable("id") id: UUID
    ): ResponseEntity<Any> {

        data[id] = "Data for $id"
        return ResponseEntity.status(200).build()
    }

    @DeleteMapping(path = ["/{id}"])
    open fun delete(@PathVariable("id") id: UUID): ResponseEntity<String> {

        if(!data.containsKey(id)){
            return ResponseEntity.status(404).build()
        }

        data.remove(id)

        return ResponseEntity.status(204).build()
    }
}