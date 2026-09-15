package com.foo.rest.examples.spring.openapi.v3.arazzo

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.atomic.AtomicLong

@RestController
open class ArazzoPetCouponsRest {

    private fun samplePet(): PetDto = PetDto(
        id = 1,
        name = "doggie",
        photoUrls = listOf("http://example.com/photo"),
        price = 9.99,
    )

    @GetMapping("/pet/findByTags")
    open fun findPetsByTags(
        @RequestParam(required = false) tags: List<String>?,
    ): ResponseEntity<List<PetDto>> = ResponseEntity.ok(listOf(samplePet()))

    @GetMapping("/pet/findByStatus")
    open fun findPetsByStatus(
        @RequestParam(required = false) status: String?,
        @RequestParam page: Int,
        @RequestParam(required = false, defaultValue = "10") pageSize: Int?,
    ): ResponseEntity<List<PetDto>> = ResponseEntity.ok(listOf(samplePet()))

    @GetMapping("/pet/{petId}/coupons")
    open fun getPetCoupons(@PathVariable petId: Long): ResponseEntity<CouponDto> =
        ResponseEntity.ok(CouponDto(couponCode = "SUMMERSALE"))

    @PostMapping(path = ["/store/order"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun placeOrder(@RequestBody order: OrderDto): ResponseEntity<OrderDto> =
        ResponseEntity.ok(order.copy(id = 1))
}
