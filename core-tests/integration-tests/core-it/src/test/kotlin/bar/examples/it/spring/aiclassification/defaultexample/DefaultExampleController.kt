package bar.examples.it.spring.aiclassification.defaultexample

import bar.examples.it.spring.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class DefaultExampleController : SpringController(DefaultExampleApplication::class.java){

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/openapi/defaultexample.json",
            null
        )
    }
}

