package com.foo.rest.examples.spring.openapi.v3.extraquery

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URLDecoder
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping(path = ["/api/extraquery"])
class ExtraQueryRest {

    @GetMapping("servlet")
    open fun servlet(hr: HttpServletRequest): String {

        val a = hr.parameterMap["a"]!![0]

        return if (a == null) {
            "FALSE"
        } else "OK"
    }

    @GetMapping("proxyprint")
    open fun proxyprint(hr: HttpServletRequest): String {

        val map = hr.parameterMap
        val payerEmail= map["payer_email"]!![0]
        val quantity= java.lang.Double.valueOf(map["mc_gross"]!![0])
        val paymentStatus= map["payment_status"]!![0]

        return if (payerEmail == null || quantity== null || paymentStatus == null) {
            "FALSE"
        } else "OK"
    }


    @GetMapping("languagetool")
    open fun languagetool(hr: HttpServletRequest): String {

        val query = hr.queryString
        val params = getParameterMap(query)

        val a = params["a"]

        return if (a == null) {
            "FALSE"
        } else "OK"
    }

    @PostMapping
    open fun post(){
        //nothing needed to do, just make sure hidden filter is used
    }

    private fun getParameterMap(query: String): Map<String, String> {
        val pairs = query.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val parameters: MutableMap<String, String> = HashMap()
        for (pair: String in pairs) {
            val delimPos = pair.indexOf('=')
            if (delimPos != -1) {
                val param = pair.substring(0, delimPos)
                val key = URLDecoder.decode(param, "utf-8")
                try {
                    parameters[key] = URLDecoder.decode(pair.substring(delimPos + 1), "utf-8")
                } catch (e: IllegalArgumentException) {
                    throw RuntimeException("Could not decode query. Query length: " + query.length, e)
                }
            }
        }
        return parameters
    }
}