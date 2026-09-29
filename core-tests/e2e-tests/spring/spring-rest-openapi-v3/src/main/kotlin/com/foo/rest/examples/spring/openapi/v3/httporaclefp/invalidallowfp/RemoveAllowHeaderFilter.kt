package com.foo.rest.examples.spring.openapi.v3.httporaclefp.invalidallowfp

import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper
import org.springframework.stereotype.Component

@Component
class RemoveAllowHeaderFilter : Filter {

    override fun init(filterConfig: FilterConfig?) {
        // no-op
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        chain.doFilter(request, object : HttpServletResponseWrapper(response as HttpServletResponse) {
            override fun setHeader(name: String, value: String) {
                if (!name.equals("Allow", ignoreCase = true)) {
                    super.setHeader(name, value)
                }
            }

            override fun addHeader(name: String, value: String) {
                if (!name.equals("Allow", ignoreCase = true)) {
                    super.addHeader(name, value)
                }
            }
        })
    }

    override fun destroy() {
        // no-op
    }
}