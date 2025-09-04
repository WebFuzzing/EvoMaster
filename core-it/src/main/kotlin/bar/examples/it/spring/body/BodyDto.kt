package bar.examples.it.spring.body

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

class BodyDto(

//    @get:Schema(
//        requiredMode = Schema.RequiredMode.REQUIRED,
//        description = "this is a required boolean field",
//        )
    @get:JsonProperty(required = true)
    var rb : Boolean? = null,

    var s: String? = null,

    @get:JsonProperty(required = true)
    var ri: Int? = null,

    var d: Double? = null,

    var l: List<String>? = null,

    var o: BodyDto? = null
)