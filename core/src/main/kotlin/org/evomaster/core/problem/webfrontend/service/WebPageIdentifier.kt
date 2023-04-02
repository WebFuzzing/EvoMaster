package org.evomaster.core.problem.webfrontend.service


/**
 * A page is identified by its HTML shape.
 * But those are potentially very large strings.
 * so we use compressed ids to represent them.
 * But, then, we need a way to get back shape from id
 */
class WebPageIdentifier {

    /**
     * Map from artificial id (eg "P1") to HTML page shape
     */
    private val idToShape: MutableMap<String, String> = mutableMapOf()

    private val shapeToId: MutableMap<String, String> = mutableMapOf()

    private var counter = 0

    fun registerShape(shape: String) : String{

        var id = shapeToId[shape]
        if(id != null){
            //already registered
            return id
        }

        id = "P$counter"
        counter++
        shapeToId[shape] = id
        idToShape[id] = shape

        return id
    }
}