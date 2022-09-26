package org.evomaster.core.search.gene.uri


import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URI
import java.net.URL

internal class UrlUriTest{


    @Test
    fun testUrl(){

        assertThrows<Exception>{ URL("foo.com")}

        URL("http://foo.com")
        URL("http:///foo")
        URL("http:////foo")
        URL("http:////foo//")
        URL("http:///foo.com")
        URL("http://@foo.com")
        URL("http://hi@foo.com")
        URL("http://hi:secret@bar.no.co.foo")
        URL("https://hi:@foo.com")
        URL("http://foo.com/hello")
        URL("http://foo.com/hello/there")
        URL("http://foo.com/hello?x=42")
        URL("http://foo.com/?x=42")
        URL("http://foo.com/hello?x=42&y=foo")
        URL("http://foo.com/hello?x=42&y=foo#aaaaaaaa")

        URL("http://127.0.0.1")
        URL("http://@127.0.0.1")
        URL("https://hi@127.0.0.1")
        URL("https://127.0.0.1/?x=42")
    }


    @Test
    fun testUri(){

        URI("foo.com")

        URI("http://foo.com")
        URI("http://@foo.com")
        URI("http://hi@foo.com")
        URI("http://hi:secret@bar.no.co.foo")
        URI("https://hi:@foo.com")
        URI("http://foo.com/hello")
        URI("http://foo.com/hello/there")
        URI("http://foo.com/hello?x=42")
        URI("http://foo.com/?x=42")
        URI("http://foo.com/hello?x=42&y=foo")
        URI("http://foo.com/hello?x=42&y=foo#aaaaaaaa")

        URI("//foo.com")
        URI("@foo.com")
        URI("hi@foo.com")
        URI("hi:secret@bar.no.co.foo")
        URI("//hi:@foo.com")
        URI("foo.com/hello")
        URI("//foo.com/hello/there")
        URI("foo.com/hello?x=42")
        URI("//foo.com/?x=42")
        URI("foo.com/hello?x=42&y=foo")
        URI("foo.com/hello?x=42&y=foo#aaaaaaaa")


        URI("data:,")
        URI("data:,foo")
        URI("data:,foo1111111")
        URI("data:text/plain;charset=US-ASCII,")
        URI("data:text/plain;charset=US-ASCII,bar")
        URI("data:text/plain;base64;charset=US-ASCII,bar")
        URI("data:;base64,")
    }

}