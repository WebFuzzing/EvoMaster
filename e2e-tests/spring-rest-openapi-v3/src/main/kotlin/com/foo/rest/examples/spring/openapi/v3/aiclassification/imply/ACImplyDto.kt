package com.foo.rest.examples.spring.openapi.v3.aiclassification.imply

import com.foo.rest.examples.spring.openapi.v3.aiclassification.allornone.ACAllOrNoneEnum
import com.foo.rest.examples.spring.openapi.v3.aiclassification.zeroorone.ACZeroOrOneEnum


class ACImplyDto(

    var a : Boolean? = null,

    var b : String? = null,

    var c : Long? = null,

    var d : ACImplyEnum? = null,

    var e : List<String>? = null,

    var f : ACImplyEnum? = null,
    )
