package com.foo.rest.examples.spring.openapi.v3.extraquery

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
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

}