package com.foo.graphql.alllimitreached

import com.foo.graphql.SpringController


class allLimitReachedController : SpringController(GQLAllLimitReachedApplication::class.java) {

    override fun schemaName() = GQLAllLimitReachedApplication.SCHEMA_NAME

}