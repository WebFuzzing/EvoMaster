package org.evomaster.core.problem.rest.resource

class SamplerSpecification (val methodKey : String, val withDependency: Boolean = false){
    fun copy() : SamplerSpecification{
        return SamplerSpecification(methodKey, withDependency)
    }
}

class MutatorSpecification (val methodKey : String, val withDependency: Boolean = false){
    fun copy() : MutatorSpecification{
        return MutatorSpecification(methodKey, withDependency)
    }
}