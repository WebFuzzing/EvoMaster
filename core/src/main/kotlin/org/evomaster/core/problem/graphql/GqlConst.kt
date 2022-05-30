package org.evomaster.core.problem.graphql

object GqlConst {

    /**
     * This tag is for the GQL union type. Needed in getValueAsPrintableString to print out things like:
     *   fieldXName{
     *        ... on UnionObject1 {
     *           field
     *        }
     *        ... on UnionObjectN {
     *          field
     *        }
     *   }
     */
    const val UNION_TAG = "#UNION#"
    /**
     * Is used for the GQL interface type. Needed in getValueAsPrintableString to print out: field1 ... fieldN  in:
     *     fieldXName{
     *        field1
     *        fieldN
     *        ... on InterfaceObject1 {
     *           field
     *        }
     *        ... on InterfaceObjectN {
     *          field
     *        }
     *     }
     */
    const val INTERFACE_BASE_TAG = "#BASE#"
    /**
     * Is used for the GQL interface type. Needed in getValueAsPrintableString to print out: ... on InterfaceObject1 ... on InterfaceObjectN in :
     *     fieldXName{
     *        field1
     *        fieldN
     *        ... on InterfaceObject1 {
     *           field
     *        }
     *        ... on InterfaceObjectN {
     *          field
     *        }
     *     }
     */
    const val INTERFACE_TAG = "#INTERFACE#"
    const val SCALAR = "scalar"
    const val OBJECT = "object"
    const val UNION = "union"
    const val INTERFACE = "interface"
    const val ENUM = "enum"
    const val LIST = "list"
    const val INPUT_OBJECT = "input_object"
    /*
    *Those are entry points of GraphQL query and mutation
    * Todo Currently, they are used to calculate statistic from the graph. Need to be generalised.
     */
    const val QUERY = "query"
    const val QUERY_TYPE = "querytype"
    const val ROOT = "root"
    const val MUTATION = "mutation"
}