package org.evomaster.core.problem.webfrontend

import org.jsoup.Jsoup
import org.jsoup.nodes.Node
import org.jsoup.select.NodeVisitor
import java.net.URL

object HtmlUtils {


    fun getUrlInALinks(html: String) : List<String>? {
        val document = try{
            Jsoup.parse(html)
        }catch (e: Exception){
            return null
        }

        return document.getElementsByTag("a")
            .map { it.attr("href")}
    }

    fun getPathAndQueries(url: String) : String{
        val x = URL(url)
        return "${x.path}${x.query ?: ""}"
    }

    /**
     * Given HTML pages, we want to identify which ones are "unique".
     * This is based on the "shape", and not actual content, as same page can be instantiated with different
     * input data (eg, think of a page showing user's info).
     * So we extract the shape of the HTML tree, with attribute names (no values), and no text content.
     */
    fun computeIdentifyingShape(html: String) : String{

        val document = try{
            Jsoup.parse(html)
        }catch (e: Exception){
            return "INVALID_HTML"
        }

        val buffer = StringBuffer(html.length)

        document.traverse(object: NodeVisitor{
            override fun head(node: Node, depth: Int) {
                val name = node.nodeName()
                if(name == "#text"){
                    buffer.append("text")
                } else {
                    buffer.append("<$name${node.attributes().joinToString("") { " ${it.key}" }}>")
                }
            }
            override fun tail(node: Node, depth: Int) {
                val name = node.nodeName()
                if(name != "#text") {
                    buffer.append("</$name>")
                }
            }
        })

        return buffer.toString()
    }

}