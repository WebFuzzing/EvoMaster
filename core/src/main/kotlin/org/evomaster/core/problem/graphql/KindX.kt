package org.evomaster.core.problem.graphql

import org.evomaster.core.problem.graphql.schema.InputValue
import org.evomaster.core.problem.graphql.schema.__Field
import org.evomaster.core.problem.graphql.schema.__TypeKind

/**
 * This class is used to compute and store kindX (to avoid code duplication in the GQL Action Builder). Where kindX is extracted from a schema.
 */
class KindX(var kind0: __TypeKind?, var kind1: __TypeKind?, var kind2: __TypeKind?, var kind3: __TypeKind?) {

    fun quadKinds(elementInfields: __Field) {
        kind0 = elementInfields?.type?.kind
        kind1 = elementInfields?.type?.ofType?.kind
        kind2 = elementInfields?.type?.ofType?.ofType?.kind
        kind3 = elementInfields?.type?.ofType?.ofType?.ofType?.kind
    }

    fun quadKindsInInputs(elementInfields: InputValue) {
        kind0 = elementInfields?.type?.kind
        kind1 = elementInfields?.type?.ofType?.kind
        kind2 = elementInfields?.type?.ofType?.ofType?.kind
        kind3 = elementInfields?.type?.ofType?.ofType?.ofType?.kind
    }
}