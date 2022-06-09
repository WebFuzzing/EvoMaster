package org.evomaster.core.problem.rest

import com.google.inject.Injector
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.problem.rest.service.ResourceRestModule

class SamplerMutationVerifierTest {


    private fun getInjector(args: List<String> = listOf()) : Injector{

        val base = BaseModule(args.toTypedArray())
        val problemModule = ResourceRestModule()

        return LifecycleInjector.builder()
                .withModules(base, problemModule)

                .build()
                .createInjector()
    }
}