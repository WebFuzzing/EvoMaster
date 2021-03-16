package com.foo.graphql.cycle.resolver

import com.foo.graphql.cycle.DataRepository
import com.foo.graphql.cycle.type.Bouquet
import com.foo.graphql.cycle.type.Store
import graphql.kickstart.tools.GraphQLResolver
import org.springframework.stereotype.Component

@Component
class BouquetResolver (private val dataRepo: DataRepository): GraphQLResolver<Bouquet> {

    fun getId(bouquet: Bouquet) = bouquet.id
    fun getName(bouquet: Bouquet) = bouquet.name
    fun getPot(bouquet: Bouquet) = bouquet.pot
    fun getStore(bouquet: Bouquet): Store? {
        return dataRepo.findStoreById(bouquet.storeId)
    }


}



/*
class PostResolver(
        private val repository: DataRepository

) : GraphQLResolver<PostType> {

    fun getId(post: PostType) = post.id.toString()


    fun getAuthor(post: PostType): AuthorType? {

        return repository.findAuthorById(post.authorId)
    }

}
    /**
     * Note: this might be an expensive operation, as every single
     * comment in the repository has to be scan
     */
    fun getComments(post: PostType) : List<CommentType>{

        return repository.getComments{it -> it.postId == post.id}
    }

    fun getComments(customFilter: ((CommentType) -> Boolean)? = null): List<CommentType> {

        if (customFilter != null) {
            return comments.values.filter(customFilter).toList()
        }

        return comments.values.toList()
    }

 */
