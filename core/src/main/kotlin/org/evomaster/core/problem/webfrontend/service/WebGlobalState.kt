package org.evomaster.core.problem.webfrontend.service

import java.net.URI
import java.net.URISyntaxException

/**
 * Keep track of some global state throughout whole search which is specific to Web Applications
 */
class WebGlobalState {

    private val urlsOfPagesWithBrokenHtml : MutableSet<String> = mutableSetOf()

    /**
     * key -> malformed URI, found in for example in <a> links
     * value -> set of urls of pages in which some malformed link was present
     */
    private val malformedLinks : MutableMap<String,MutableSet<String>> = mutableMapOf()

    /**
     * key -> URI  found in <a> links pointing to external hosts
     * value -> set of urls of pages in which the link is present
     */
    private val externalLinks :  MutableMap<URI,MutableSet<String>> = mutableMapOf()

    fun addBrokenPage(url: String) = urlsOfPagesWithBrokenHtml.add(url)

    fun getBrokenPageUrls() : Set<String> = urlsOfPagesWithBrokenHtml

    fun addMalformedUri(uri: String, urlOfPageWithTheLink: String){
        try {
            URI(uri)
            throw IllegalArgumentException("Input URI is not malformed: $uri")
        }catch (e: URISyntaxException){
            //expected
        }
        val origins = malformedLinks.getOrPut(uri) { mutableSetOf() }
        origins.add(urlOfPageWithTheLink)
    }

    fun getMalformedLinks() : Map<String,Set<String>> = malformedLinks


    fun addExternalLink(uri: URI, urlOfPageWithTheLink: String){

        if(uri.host.isNullOrBlank()){
            throw IllegalArgumentException("URI is missing host info: $uri")
        }

        val origins = externalLinks.getOrPut(uri) { mutableSetOf() }
        origins.add(urlOfPageWithTheLink)
    }

    fun getExternalLinks() : Map<URI,Set<String>> = externalLinks
}