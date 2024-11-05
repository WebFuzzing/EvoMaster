package com.foo.rest.examples.spring.openapi.v3.jackson.complex


class ContactDto {
    var contactElements : MutableMap<String, MutableList<ContactElementDto>> = mutableMapOf()

}
