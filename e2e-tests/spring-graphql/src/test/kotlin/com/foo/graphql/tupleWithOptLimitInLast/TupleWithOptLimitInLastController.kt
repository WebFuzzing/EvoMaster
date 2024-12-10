package com.foo.graphql.tupleWithOptLimitInLast

import com.foo.graphql.SpringController



class TupleWithOptLimitInLastController : SpringController(TupleWithOptLimitInLastApplication::class.java) {

    override fun schemaName() = TupleWithOptLimitInLastApplication.SCHEMA_NAME


}
