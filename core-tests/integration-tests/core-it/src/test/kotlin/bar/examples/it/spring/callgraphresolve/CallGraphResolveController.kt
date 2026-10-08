package bar.examples.it.spring.callgraphresolve

import bar.examples.it.spring.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class CallGraphResolveController : SpringController(CallGraphResolveApplication::class.java){

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/openapi/callgraphresolve.yaml",
            null
        )
    }

}
