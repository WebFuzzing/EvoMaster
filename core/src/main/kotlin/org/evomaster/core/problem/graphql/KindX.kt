package org.evomaster.core.problem.graphql

import org.evomaster.core.problem.graphql.schema.InputValue
import org.evomaster.core.problem.graphql.schema.__Field

class KindX {

        fun quadKinds(elementInfields: __Field): QuadComponent {

            val kind0 = elementInfields?.type?.kind
            val kind1 = elementInfields?.type?.ofType?.kind
            val kind2 = elementInfields?.type?.ofType?.ofType?.kind
            val kind3 = elementInfields?.type?.ofType?.ofType?.ofType?.kind

            return QuadComponent(kind0, kind1, kind2, kind3)
        }

    fun quadKindsInInputs(elementInfields: InputValue): QuadComponent {

        val kind0 = elementInfields?.type?.kind
        val kind1 = elementInfields?.type?.ofType?.kind
        val kind2 = elementInfields?.type?.ofType?.ofType?.kind
        val kind3 = elementInfields?.type?.ofType?.ofType?.ofType?.kind

        return QuadComponent(kind0, kind1, kind2, kind3)
    }
}