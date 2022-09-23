package com.foo.graphql.allLimitReached

import com.foo.graphql.SpringController


class allLimitReachedController : SpringController(GQLAllLimitReachedApplication::class.java) {

    override fun schemaName() = GQLAllLimitReachedApplication.SCHEMA_NAME

}