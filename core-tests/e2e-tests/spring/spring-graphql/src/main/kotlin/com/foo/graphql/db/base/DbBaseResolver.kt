package com.foo.graphql.db.base

import graphql.kickstart.tools.GraphQLResolver
import org.springframework.stereotype.Component

@Component
class DbBaseResolver(
        private val postRepository: DbBaseRepository
) : GraphQLResolver<DbBase> {

    fun getId(post: DbBase) = post.id.toString()

    fun getName(post: DbBase) = post.name
}