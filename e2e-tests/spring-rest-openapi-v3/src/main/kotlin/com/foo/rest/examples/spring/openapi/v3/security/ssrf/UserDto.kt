package com.foo.rest.examples.spring.openapi.v3.security.ssrf

import io.swagger.v3.oas.annotations.media.Schema

class UserDto {

    @Schema(name = "userId", example = "12-abd", required = true, description = "User ID")
    var userId: String? = null

    @Schema(
        name = "profileImageUrl",
        example = "http://example.com/profile.jpg",
        required = true,
        description = "Profile image remote URL"
    )
    var profileImageUrl: String? = null
}
