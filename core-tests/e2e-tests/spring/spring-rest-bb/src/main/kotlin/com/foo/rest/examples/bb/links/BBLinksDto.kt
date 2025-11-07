package com.foo.rest.examples.bb.links

open class BBLinksDto(

    var data: BBLinksDataDto? = null,

    var errrors : String? = null,

)

open class BBLinksDataDto(

    var id: String? = null,

    var code: Int? = null
)