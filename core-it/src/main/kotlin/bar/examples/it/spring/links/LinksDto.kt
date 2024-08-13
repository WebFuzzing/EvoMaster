package bar.examples.it.spring.links

 open class LinksDto(

     var data: LinksDataDto? = null,
     var errors: String? = null
 )

open class LinksDataDto(
    var id: String? = null,
    var name: String? = null
)