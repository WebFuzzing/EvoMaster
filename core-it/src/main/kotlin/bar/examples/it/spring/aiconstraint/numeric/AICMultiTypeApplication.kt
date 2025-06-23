package bar.examples.it.spring.aiconstraint.numeric

import io.swagger.v3.oas.annotations.Parameter
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/petShopApi"])
@RestController
open class AICMultiTypeApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(AICMultiTypeApplication::class.java, *args)
        }
    }

    enum class Category {
        CAT,
        DOG,
        BIRD,
        REPTILE,
        OTHER
    }

    enum class Gender {
        MALE,
        FEMALE
    }

    @GetMapping("/petInfo")
    open fun getString(

        @RequestParam("category", required = true)
        @Parameter(required = true, description = "Pet category")
        category: Category,

        @RequestParam("gender", required = true)
        @Parameter(required = true, description = "Pet gender")
        gender: Gender,

        @RequestParam("birthYear", required = true)
        @Parameter(
            required = true,
            description = "Pet birth year",
            // TODO
            // setting min, max or example causes the evomaster recognizes the gene as a ChoiceGene
            // this challenge need to be explored later
            //schema = Schema(minimum = "1900", maximum = "2025"),
        )
        birthYear: Int,

        @RequestParam("vaccinationYear", required = true)
        @Parameter(
            required = true,
            description = "Pet vaccination year",
        )
        vaccinationYear: Int,

        @RequestParam("isAlive", required = true)
        @Parameter(required = true, description = "Pet is alive?")
        isAlive: Boolean,

        @RequestParam("weight", required = true)
        @Parameter(
            required = true,
            description = "Pet weight",
        )
        weight: Double

    ): ResponseEntity<String> {

        // Dependencies
        if (birthYear <= 0) {
            return ResponseEntity.status(400).body("Birth year must be a positive number.")
        }
        if (vaccinationYear <= 0) {
            return ResponseEntity.status(400).body("Vaccination year must be a positive number.")
        }
        if (vaccinationYear < birthYear) {
            return ResponseEntity.status(400).body("Vaccination year cannot be earlier than birth year.")
        }

        // Response
        return ResponseEntity.status(200).body(
            "Birth Year: $birthYear, Vaccination Year: $vaccinationYear, Gender: $gender, Is Alive: $isAlive"
        )
    }

    @GetMapping("/ownerInfo")
    open fun getOwnerInfo(
        @RequestParam("name", required = true)
        @Parameter(required = true, description = "Owner's name")
        name: String,

        @RequestParam("age", required = true)
        @Parameter(required = true, description = "Owner's age")
        age: Int

    ): ResponseEntity<String> {

        if (age <= 0) {
            return ResponseEntity.status(400).body("Owner age must be a positive number.")
        }

        // Response
        return ResponseEntity.status(200).body(
            "Owner Name: $name, Age: $age"
        )

    }

}