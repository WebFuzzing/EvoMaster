package bar.examples.it.spring.callgraphdelete

import bar.examples.it.spring.SpringController
import bar.examples.it.spring.links.LinksApplication
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class CallGraphDeleteController : SpringController(CallGraphDeleteApplication::class.java){

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/openapi/callgraphdelete.yaml",
            null
        )
    }

}