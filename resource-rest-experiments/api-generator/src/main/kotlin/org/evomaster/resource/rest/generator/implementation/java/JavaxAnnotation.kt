package org.evomaster.resource.rest.generator.implementation.java

import org.evomaster.resource.rest.generator.template.RelationTag
import org.evomaster.resource.rest.generator.template.Tag

/**
 * created by manzh on 2019-08-15
 *
 * only used annotations are included.
 */
object JavaxAnnotation {

    // from javax.persistence

    /**
     * Specifies the primary key of an entity.
     */
    val ID = Tag("Id")

    /**
     * Designates a class whose mapping information is applied to the entities that inherit from it.
     */
    val MAPPED_SUPPERCLASS = Tag("MappedSuperclass")

    /**
     * Specifies that the class is an entity.
     */
    val ENTITY = Tag("Entity")

    /**
     * Specifies a single-valued association to another entity that has one-to-one multiplicity.
     */
    val ONE_TO_ONE = RelationTag("OneToOne")

    /**
     * Specifies a single-valued association to another entity class that has many-to-one multiplicity.
     */
    val MANY_TO_ONE = Tag("ManyToOne")

    /**
     * Specifies a many-valued association with one-to-many multiplicity.
     */
    val ONE_TO_MANY = RelationTag("OneToMany")
    /**
     * Specifies a many-valued association with many-to-many multiplicity.
     */
    val MANY_TO_MANY = Tag("ManyToMany")

    /**
     * Provides for the specification of generation strategies for the values of primary keys.
     */
    val AUTO_GEN_VALUE = Tag("GeneratedValue")


    val Table = object : Tag("Table"){

        private val params = listOf("name", "schema")

        override fun validateParams(param : String): Boolean = params.contains(param)
    }


    // from javax.validation.constraints

    /**
     * The annotated element must not be null.
     */
    val NOT_Null = Tag("NotNull")

    fun requiredPackages() : Array<String> = arrayOf(
            "javax.persistence.*",
            "javax.validation.constraints.*"
    )
}