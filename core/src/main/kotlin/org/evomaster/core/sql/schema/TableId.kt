package org.evomaster.core.sql.schema

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.api.dto.database.schema.TableIdDto

/**
 * MUST be kept immutable
 */
data class TableId(

    /**
     * The name of table
     */
    val name: String,

    /**
     * An id representing the connection to the database process.
     * This could be the connecting URL.
     * This is needed when dealing with multiple connection to different database processes,
     * possibly running on different machines.
     * If left null, default connection will be used (ie, assume there is only one).
     */
    val connectionId: String? = null,

    /**
     * At a high-level this represents a "catalog".
     * Ie, a "logical" database instance.
     * Note that a database process could have several logical databases.
     * We do not use the term "catalog" though as Postgres and MySQL behave differently.
     * In MySQL, can access different catalogs on same connection.
     * This is not possible on Postgres.
     * However, this latter has "schemas", which MySQL does not.
     * In other words:
     * - MySQL: this will always be empty
     * - Postgres: this would be the catalog name.
     * However, as in Postgres we cannot change catalog within same connection, the important info is in the
     * [connectionId] entry.
     * Even if we were to access 2 different catalogs in the same database process, we would need 2 different connections.
     * As such, this parameter is technically redundant.
     */
    val sealedGroupName: String? = null,

    /**
     *  In Postgres this represents a "schema", whereas it is a "catalog" for MySQL.
     *  In other words, this is used to group tables that can be indexed by this group name for disambiguation.
     */
    val openGroupName: String? = null,
){

    init {
        if(name.contains(".")){
            throw IllegalArgumentException("Name contains a '.'. " +
                    "You sure you passed a simple name and not a fully qualified one? Value: $name")
        }
    }

    companion object{

        fun fromDto(type: DatabaseType, dto: TableIdDto) : TableId{

            val connectionId = null //TODO

            return when(type){
                DatabaseType.POSTGRES, DatabaseType.H2 ->
                    TableId(dto.name, connectionId, dto.catalog, dto.schema)

                DatabaseType.MYSQL, DatabaseType.MARIADB ->
                    TableId(dto.name, connectionId, null, dto.schema ?: dto.catalog)

                else ->
                    TableId(dto.name, connectionId, dto.catalog, dto.schema)
            }
        }
    }


    fun getFullQualifyingTableName() : String{
        if(openGroupName.isNullOrBlank()){
            return name
        }
        return "$openGroupName.${name}"
    }
}
