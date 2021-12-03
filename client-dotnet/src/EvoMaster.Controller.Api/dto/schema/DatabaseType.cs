namespace EvoMaster.Controller.Api {
    //TODO: Review possible values for this enum
    public enum DatabaseType {
        // discussed it a bit with Andrea, might remove this one for .net
        H2,

        DERBY,

        MYSQL,

        POSTGRES,

        MARIADB,

        MS_SQL_SERVER,

        /**
    * In case used database is not listed in this enum, can
    * still try to build SQL queries, although cannot guarantee
    * that it would be correct (ie, wrong dialect).
    */
        OTHER
    }
}