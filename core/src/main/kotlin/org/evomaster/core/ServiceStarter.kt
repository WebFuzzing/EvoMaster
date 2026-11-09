package org.evomaster.core

import com.google.inject.Injector
import com.google.inject.Scope
import com.google.inject.Scopes
import com.google.inject.spi.BindingScopingVisitor
import java.lang.reflect.Method
import javax.inject.Singleton

/**
 * Unfortunately, Governator does not guarantee that @PostConstruct of injected dependencies is resolved.
 * And, even if it would, it would break on cycles, that we have.
 *
 * So, we created a new concept of [StartService] executed AFTER the @PostConstruct of Governator.
 * For those, dependencies on other started services must be explicitly stated, and cycles are NOT allowed
 * for the start methods.
 * There can still be cycles in @PostConstruct though (those would not be a problem).
 *
 */
object ServiceStarter {

    private val singletonVisitor = object : BindingScopingVisitor<Boolean> {
        override fun visitEagerSingleton(): Boolean = true
        override fun visitScope(scope: Scope): Boolean = scope == Scopes.SINGLETON
        override fun visitScopeAnnotation(a: Class<out Annotation>): Boolean = a == Singleton::class.java
        override fun visitNoScoping(): Boolean = false
    }

    private fun singletonInstances(injector: Injector): List<Any> =
        injector.allBindings.values
            .filter { it.acceptScopingVisitor(singletonVisitor) }
            .map { it.provider.get() }
            .distinctBy { System.identityHashCode(it) }

    /** All methods on [instance]'s class hierarchy annotated with @StartService. */
    private fun startMethods(instance: Any): List<Method> =
        instance.javaClass.methods.filter { it.isAnnotationPresent(StartService::class.java) }

    /**
     * For each singleton that has at least one @StartService method, collect the set of
     * concrete singleton classes it declares as dependencies via dependsOn.
     * Dependencies not present in the singleton set are ignored (they have nothing to start).
     */
    private fun buildGraph(instances: List<Any>): Map<Any, Set<Any>> {
        // Map from declared dependency class -> the singleton instances assignable to it
        val graph = mutableMapOf<Any, MutableSet<Any>>()
        for (instance in instances) {
            val methods = startMethods(instance)
            if (methods.isEmpty()) continue
            val deps = mutableSetOf<Any>()
            for (m in methods) {
                val ann = m.getAnnotation(StartService::class.java)
                for (depClass in ann.dependsOn) {
                    val javaClass = depClass.java
                    // Find singleton instances whose runtime class is assignable to depClass
                    for (other in instances) {
                        if (other === instance) continue
                        if (javaClass.isAssignableFrom(other.javaClass)) {
                            deps.add(other)
                        }
                    }
                }
            }
            graph[instance] = deps
        }
        return graph
    }

    /**
     * Kahn's algorithm. If any nodes remain unsorted at the end, there is a cycle;
     * throw with the cycle members listed.
     */
    private fun topoSort(graph: Map<Any, Set<Any>>): List<Any> {
        val inDegree = graph.keys.associateWith { 0 }.toMutableMap()
        val dependents = mutableMapOf<Any, MutableSet<Any>>()
        for ((node, deps) in graph) {
            for (d in deps) {
                dependents.getOrPut(d) { mutableSetOf() }.add(node)
                inDegree[node] = inDegree.getValue(node) + 1
            }
        }
        val queue = ArrayDeque(inDegree.filterValues { it == 0 }.keys)
        val sorted = mutableListOf<Any>()
        while (queue.isNotEmpty()) {
            val n = queue.removeFirst()
            sorted.add(n)
            for (dep in dependents[n].orEmpty()) {
                inDegree[dep] = inDegree.getValue(dep) - 1
                if (inDegree.getValue(dep) == 0) queue.add(dep)
            }
        }
        check(sorted.size == graph.size) {
            "Cycle detected in @StartService dependencies among: " +
                    "${(graph.keys - sorted.toSet()).map { it.javaClass.name }}"
        }
        return sorted
    }

    /**
     * Find all singletons with @StartService methods, order them by their dependsOn
     * declarations, and invoke the methods in that order. Crashes on cycles.
     */
    fun startAllServices(injector: Injector) {
        val instances = singletonInstances(injector)
        val graph = buildGraph(instances)
        val ordered = topoSort(graph)
        for (instance in ordered) {
            for (method in startMethods(instance)) {
                method.isAccessible = true
                method.invoke(instance)
            }
        }
    }
}