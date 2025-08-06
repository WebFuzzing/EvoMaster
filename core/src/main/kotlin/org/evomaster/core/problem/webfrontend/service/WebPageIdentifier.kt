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

    fun registerShape(html: String) : String{

        val shape = html
        //normalizeHtml(html); -> if needed, should go to HtmlUtils.computeIdentifyingShape
        var id = shapeToId[shape]
        if(id != null){
            //already registered
            return id
        }
        // shape is new  -> generate id and update both maps
        id = "P$counter"
        counter++
        shapeToId[shape] = id
        idToShape[id] = shape

        return id
    }

    // normalize html -> remove white spaces and dynamic ids
    private fun normalizeHtml(html: String): String {
        return html
            .replace(Regex("\\s+"), " ") // Collapse whitespace
            .replace(Regex("id=\"[^\"]+\""), "id=\"*\"") // Remove dynamic IDs
    }
}