package com.foo.rest.examples.spring.openapi.v3.dtoreflectiveassert

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class DtoReflectiveAssertRest {

    @PostMapping(path = ["/allof"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun allof(@RequestBody body: AllOfDto) : ResponseEntity<String>{
        return ResponseEntity.ok("OK")
    }

    // TODO: Restore when support for ChoiceGene has been added
//    @PostMapping(path = ["/anyof"], consumes = [MediaType.APPLICATION_JSON_VALUE])
//    open fun anyof(@RequestBody body: AnyOfDto) : ResponseEntity<String>{
//        return ResponseEntity.ok("OK")
//    }

    // TODO: Restore when support for ChoiceGene has been added
//    @PostMapping(path = ["/oneof"], consumes = [MediaType.APPLICATION_JSON_VALUE])
//    open fun oneof(@RequestBody body: OneOfDto) : ResponseEntity<String>{
//        return ResponseEntity.ok("OK")
//    }

    @PostMapping(path = ["/primitiveTypes"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun primitiveTypes(@RequestBody body: PrimitiveTypesDto) : ResponseEntity<String>{
        return ResponseEntity.ok("OK")
    }

    @PostMapping(path = ["/parent"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun parent(@RequestBody body: ParentSchemaDto) : ResponseEntity<String>{
        return ResponseEntity.ok("OK")
    }

    @PostMapping(path = ["/items-inline"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun itemsInline(@RequestBody body: List<ItemsInlineDto>) : ResponseEntity<String>{
        return ResponseEntity.ok("OK")
    }

}
