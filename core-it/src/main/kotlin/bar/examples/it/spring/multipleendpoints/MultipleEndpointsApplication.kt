package bar.examples.it.spring.multipleendpoints

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api"])
@RestController
open class MultipleEndpointsApplication {


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(MultipleEndpointsApplication::class.java, *args)
        }
    }


    // GET methods

    /**
     * Get endpoint 1 with identifier endpointIdentifier, returns 200 as response.
     */
    @GetMapping("/endpoint1/{endpointIdentifier}")
    open fun getByIdEndpoint1(@PathVariable endpointIdentifier: Int) : ResponseEntity<String> {

        return ResponseEntity.status(201).body("endpoint1_GET : $endpointIdentifier")
    }

    /**
     * Get endpoint 1 with the given status code as the response.
     */
    @GetMapping("/endpoint1/setStatus/{status}")
    open fun getResponseWithGivenStatusEndpoint1(@PathVariable status: Int) : ResponseEntity<String> {

        return ResponseEntity.status(status).body("endpoint1_SET_STATUS")
    }

    /**
     * Get endpoint 2 with identifier endpointIdentifier, returns 201 as response.
     */
    @GetMapping("/endpoint2/{endpointIdentifier}")
    open fun getByIdEndpoint2(@PathVariable endpointIdentifier: Int) : ResponseEntity<String> {

        return ResponseEntity.status(202).body("endpoint2_GET : $endpointIdentifier")
    }

    /**
     * Get endpoint 2 with the given status code as the response.
     */
    @GetMapping("/endpoint2/setStatus/{status}")
    open fun getResponseWithGivenStatusEndpoint2(@PathVariable status: Int) : ResponseEntity<String> {

        return ResponseEntity.status(status).body("endpoint2_SET_STATUS")
    }

    /**
     * Get endpoint 3 with identifier endpointIdentifier, returns 202 as response.
     */
    @GetMapping("/endpoint3/{endpointIdentifier}")
    open fun getByIdEndpoint3(@PathVariable endpointIdentifier: Int) : ResponseEntity<String> {

        return ResponseEntity.status(203).body("endpoint3_GET : $endpointIdentifier")
    }

    /**
     * Get endpoint 3 with the given status code as the response.
     */
    @GetMapping("/endpoint3/setStatus/{status}")
    open fun getResponseWithGivenStatusEndpoint3(@PathVariable status: Int) : ResponseEntity<String> {

        return ResponseEntity.status(status).body("endpoint3_SET_STATUS")
    }

    /**
     * Get endpoint 4 with identifier endpointIdentifier, returns 203 as response.
     */
    @GetMapping("/endpoint4/{endpointIdentifier}")
    open fun getByIdEndpoint4(@PathVariable endpointIdentifier: Int) : ResponseEntity<String> {

        return ResponseEntity.status(204).body("endpoint4_GET : $endpointIdentifier")
    }

    /**
     * Get endpoint 4 with the given status code as the response.
     */
    @GetMapping("/endpoint4/setStatus/{status}")
    open fun getResponseWithGivenStatusEndpoint4(@PathVariable status: Int) : ResponseEntity<String> {

        return ResponseEntity.status(status).body("endpoint4_SET_STATUS")
    }

    /**
     * Get endpoint 5 with identifier endpointIdentifier, returns 204 as response.
     */
    @GetMapping("/endpoint5/{endpointIdentifier}")
    open fun getByIdEndpoint5(@PathVariable endpointIdentifier: Int) : ResponseEntity<String> {

        return ResponseEntity.status(205).body("endpoint5_GET : $endpointIdentifier")
    }

    /**
     * Get endpoint 5 with the given status code as the response.
     */
    @GetMapping("/endpoint5/setStatus/{status}")
    open fun getResponseWithGivenStatusEndpoint5(@PathVariable status: Int) : ResponseEntity<String> {

        return ResponseEntity.status(status).body("endpoint5_SET_STATUS")
    }

    /**
     * POST endpoint 1, returns 301
     */
    @PostMapping("/endpoint1")
    open fun postEndpoint1() : ResponseEntity<String> {

        return ResponseEntity.status(301).body("endpoint1_POST")
    }

    /**
     * POST endpoint 2, returns 302
     */
    @PostMapping("/endpoint2")
    open fun postEndpoint2() : ResponseEntity<String> {

        return ResponseEntity.status(302).body("endpoint2_POST")
    }

    /**
     * POST endpoint 3, returns 303
     */
    @PostMapping("/endpoint3")
    open fun postEndpoint3() : ResponseEntity<String> {

        return ResponseEntity.status(303).body("endpoint3_POST")
    }

    /**
     * POST endpoint 4, returns 304
     */
    @PostMapping("/endpoint4")
    open fun postEndpoint4() : ResponseEntity<String> {

        return ResponseEntity.status(304).body("endpoint4_POST")
    }

    /**
     * POST endpoint 5, returns 305
     */
    @PostMapping("/endpoint5")
    open fun postEndpoint5() : ResponseEntity<String> {

        return ResponseEntity.status(305).body("endpoint5_POST")
    }

    /**
     * PUT endpoint 1, returns 401
     */
    @PutMapping("/endpoint1/{endpointIdentifier}")
    open fun putByIdEndpoint1(@PathVariable endpointIdentifier: Int) : ResponseEntity<String> {

        return ResponseEntity.status(401).body("endpoint1_PUT : $endpointIdentifier")
    }

    /**
     * PUT endpoint 2, returns 402
     */
    @PutMapping("/endpoint2/{endpointIdentifier}")
    open fun putByIdEndpoint2(@PathVariable endpointIdentifier: Int) : ResponseEntity<String> {

        return ResponseEntity.status(402).body("endpoint2_PUT : $endpointIdentifier")
    }

    /**
     * PUT endpoint 3, returns 403
     */
    @PutMapping("/endpoint3/{endpointIdentifier}")
    open fun putByIdEndpoint3(@PathVariable endpointIdentifier: Int) : ResponseEntity<String> {

        return ResponseEntity.status(403).body("endpoint3_PUT : $endpointIdentifier")
    }

    /**
     * PUT endpoint 4, returns 404
     */
    @PutMapping("/endpoint4/{endpointIdentifier}")
    open fun putByIdEndpoint4(@PathVariable endpointIdentifier: Int) : ResponseEntity<String> {

        return ResponseEntity.status(404).body("endpoint4_PUT : $endpointIdentifier")
    }

    /**
     * PUT endpoint 5, returns 405
     */
    @PutMapping("/endpoint5/{endpointIdentifier}")
    open fun putByIdEndpoint5(@PathVariable endpointIdentifier: Int) : ResponseEntity<String> {

        return ResponseEntity.status(405).body("endpoint5_PUT : $endpointIdentifier")
    }

    /**
     * DELETE endpoint 1, returns 501
     */
    @DeleteMapping("/endpoint1/{endpointIdentifier}")
    open fun deleteByIdEndpoint1(@PathVariable endpointIdentifier: Int) : ResponseEntity<String> {

        return ResponseEntity.status(501).body("endpoint1_DELETE : $endpointIdentifier")
    }

    /**
     * DELETE endpoint 2, returns 502
     */
    @DeleteMapping("/endpoint2/{endpointIdentifier}")
    open fun deleteByIdEndpoint2(@PathVariable endpointIdentifier: Int) : ResponseEntity<String> {

        return ResponseEntity.status(502).body("endpoint2_DELETE : $endpointIdentifier")
    }

    /**
     * DELETE endpoint 3, returns 503
     */
    @DeleteMapping("/endpoint3/{endpointIdentifier}")
    open fun deleteByIdEndpoint3(@PathVariable endpointIdentifier: Int) : ResponseEntity<String> {

        return ResponseEntity.status(503).body("endpoint3_DELETE : $endpointIdentifier")
    }

    /**
     * DELETE endpoint 4, returns 504
     */
    @DeleteMapping("/endpoint4/{endpointIdentifier}")
    open fun deleteByIdEndpoint4(@PathVariable endpointIdentifier: Int) : ResponseEntity<String> {

        return ResponseEntity.status(504).body("endpoint4_DELETE : $endpointIdentifier")
    }

    /**
     * DELETE endpoint 5, returns 505
     */
    @DeleteMapping("/endpoint5/{endpointIdentifier}")
    open fun deleteByIdEndpoint5(@PathVariable endpointIdentifier: Int) : ResponseEntity<String> {

        return ResponseEntity.status(505).body("endpoint5_DELETE : $endpointIdentifier")
    }


}