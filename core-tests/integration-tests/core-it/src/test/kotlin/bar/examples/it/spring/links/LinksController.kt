package bar.examples.it.spring.links

import bar.examples.it.spring.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class LinksController : SpringController(LinksApplication::class.java){

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/openapi/links.json",
            null
        )
    }

}