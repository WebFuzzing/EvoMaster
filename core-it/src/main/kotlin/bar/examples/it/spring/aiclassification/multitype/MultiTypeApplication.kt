package bar.examples.it.spring.aiclassification.multitype

import io.swagger.v3.oas.annotations.Parameter
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/petShopApi"])
@RestController
open class MultiTypeApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(MultiTypeApplication::class.java, *args)
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
    open fun getPetInfo(

        @RequestParam("category", required = true)
        @Parameter(required = true, description = "Pet category")
        category: Category,

        @RequestParam("gender", required = true)
        @Parameter(required = true, description = "Pet gender")
        gender: Gender,

        @RequestParam("birthDate", required = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Parameter(
            required = true,
            description = "Pet birth date (yyyy-MM-dd)",
            example = "2020-05-14"
        )
        birthDate: LocalDate,

        @RequestParam("lastVaccination", required = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(
            required = true,
            description = "Date and time of last vaccination (ISO 8601)",
            example = "2024-08-01T10:30:00"
        )
        lastVaccination: LocalDateTime,

        @RequestParam("isAlive", required = true)
        @Parameter(required = true, description = "Pet is alive?")
        isAlive: Boolean,

        @RequestParam("weight", required = true)
        @Parameter(
            required = true,
            description = "Pet weight in kg",
            example = "5.6"
        )
        weight: Double

    ): ResponseEntity<String> {

        // Validation
        if (lastVaccination.isBefore(birthDate.atStartOfDay())) {
            return ResponseEntity.status(400).body("Vaccination date cannot be before birth date.")
        }

        return ResponseEntity.status(200).body(
            "Pet Info → Birth Date: $birthDate, Last Vaccination: $lastVaccination, Gender: $gender, Alive: $isAlive, Weight: $weight"
        )
    }

    @PostMapping("/petInfo")
    open fun createPet(

        @RequestParam("category", required = true)
        @Parameter(required = true, description = "Pet category")
        category: Category,

        @RequestParam("gender", required = true)
        @Parameter(required = true, description = "Pet gender")
        gender: Gender,

        @RequestParam("birthDate", required = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Parameter(
            required = true,
            description = "Pet birth date (yyyy-MM-dd)",
            example = "2022-03-20"
        )
        birthDate: LocalDate,

        @RequestParam("lastVaccination", required = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Parameter(
            required = true,
            description = "Date and time of last vaccination (ISO 8601)",
            example = "2023-05-10T15:45:00"
        )
        lastVaccination: LocalDateTime,

        @RequestParam("isAlive", required = true)
        @Parameter(required = true, description = "Pet is alive?")
        isAlive: Boolean,

        @RequestParam("weight", required = true)
        @Parameter(
            required = true,
            description = "Pet weight in kg",
            example = "3.2"
        )
        weight: Double
    ): ResponseEntity<String> {

        // Validation
        if (weight <= 0) {
            return ResponseEntity.status(400).body("Weight must be positive.")
        }
        if (lastVaccination.isBefore(birthDate.atStartOfDay())) {
            return ResponseEntity.status(400).body("Vaccination date cannot be before birth date.")
        }

        return ResponseEntity.status(200).body(
            "Pet created successfully → Category: $category, " +
                    "Birth Date: $birthDate, Vaccination: $lastVaccination, " +
                    "Gender: $gender, Alive: $isAlive, Weight: $weight"
        )
    }
}
