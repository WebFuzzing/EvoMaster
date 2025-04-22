package com.foo.rest.examples.bb.authcookie;

import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping(path = ["/"])
class CookieDashboardRest {
    @GetMapping(path = ["/dashboard"])
    open fun check() : ResponseEntity<String>{
        return ResponseEntity.ok("OK")
    }
}
