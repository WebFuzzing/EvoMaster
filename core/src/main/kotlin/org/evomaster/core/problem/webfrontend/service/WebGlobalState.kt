package org.evomaster.core.problem.webfrontend.service

import java.net.URI
import java.net.URISyntaxException
import java.net.URL

/**
 * Keep track of some global state throughout whole search which is specific to Web Applications
 */
class WebGlobalState {

    class ExternalLinkState(
        val url: URL,
        val valid: Boolean,
        val pages : MutableSet<String> = mutableSetOf()
    )


    private val urlsOfPagesWithBrokenHtml : MutableSet<String> = mutableSetOf()

    /**
     * key -> malformed URI, found in for example in <a> links
     * value -> set of urls of pages in which some malformed link was present
     */
    private val malformedLinks : MutableMap<String,MutableSet<String>> = mutableMapOf()

    /**
     * key -> URL  found in <a> links pointing to external hosts
     * value -> set of urls of pages in which the link is present
     */
    private val externalLinks :  MutableMap<URL, ExternalLinkState> = mutableMapOf()

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


    fun addExternalLink(url: URL, valid: Boolean, urlOfPageWithTheLink: String){

        if(url.host.isNullOrBlank()){
            throw IllegalArgumentException("URL is missing host info: $url")
        }
        if(externalLinks.containsKey(url)){
            throw IllegalArgumentException("URL already registered")
        }

        val state = ExternalLinkState(url, valid)
        state.pages.add(urlOfPageWithTheLink)
        externalLinks[url] = state
    }

    fun updateExternalLink(url: URL, urlOfPageWithTheLink: String){
        externalLinks[url]?.pages?.add(urlOfPageWithTheLink)
            ?: throw IllegalArgumentException("URL is not registered: $url")
    }

    fun hasAlreadySeenExternalLink(url: URL) = externalLinks.containsKey(url)

    fun isBrokenLink(url: URL) : Boolean{
        val info = externalLinks?.get(url) ?: throw IllegalArgumentException("Not registered URL")
        return !info.valid
    }

    fun getBrokenExternalLinks() : Map<URL,Set<String>> =
        externalLinks
            .filter { !it.value.valid }
            .entries
            .associate { Pair(it.key, it.value.pages) }
}