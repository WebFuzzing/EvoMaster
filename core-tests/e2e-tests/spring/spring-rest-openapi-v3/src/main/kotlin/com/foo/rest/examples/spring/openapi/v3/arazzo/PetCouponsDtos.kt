package com.foo.rest.examples.spring.openapi.v3.arazzo

data class PetDto(
    val id: Long? = null,
    val name: String,
    val photoUrls: List<String>,
    val price: Double,
)

data class CouponDto(
    val couponCode: String,
)

data class OrderDto(
    val id: Long? = null,
)
