package com.foo.rest.examples.spring.openapi.v3.arazzo

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
open class ArazzoPetCouponsRest {

    companion object {
        private var executeFindPetsByTags: Boolean = false
        private var executeFindPetsByStatus: Boolean = false
        private var executeGetPetCoupons: Boolean = false

        @JvmStatic
        fun resetState() {
            executeFindPetsByTags = false
            executeFindPetsByStatus = false
            executeGetPetCoupons = false
        }
    }

    private fun samplePet(): PetDto = PetDto(
        id = 1,
        name = "doggie",
        photoUrls = listOf("http://example.com/photo"),
        price = 9.99,
    )

    @GetMapping("/pet/findByTags")
    open fun findPetsByTags(
        @RequestParam(required = false) tags: List<String>?,
    ): ResponseEntity<List<PetDto>> {
        executeFindPetsByTags = true
        return ResponseEntity.ok(listOf(samplePet()))
    }

    @GetMapping("/pet/findByStatus")
    open fun findPetsByStatus(
        @RequestParam(required = false) status: String?,
        @RequestParam page: Int,
        @RequestParam(required = false, defaultValue = "10") pageSize: Int?,
    ): ResponseEntity<List<PetDto>> {
        executeFindPetsByStatus = true
        return ResponseEntity.ok(listOf(samplePet()))
    }

    @GetMapping("/pet/{petId}/coupons")
    open fun getPetCoupons(@PathVariable petId: Long): ResponseEntity<CouponDto> {
        if (executeFindPetsByTags) {
            executeGetPetCoupons = true
            return ResponseEntity.ok(CouponDto(couponCode = "SUMMERSALE"))
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
    }

    @PostMapping(path = ["/store/order"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun placeOrder(@RequestBody order: OrderDto): ResponseEntity<OrderDto> {
        if (executeGetPetCoupons || executeFindPetsByStatus) {
            return ResponseEntity.ok(order.copy(id = 1))
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
    }
}
