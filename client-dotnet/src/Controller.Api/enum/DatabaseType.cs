namespace Controller.Api {

  //TODO: Review possible values for this enum
  public enum DatabaseType {

    // discussed with Andrea, remove this one for .net
    H2,

    DERBY,

    POSTGRES,

    /**
    * In case used database is not listed in this enum, can
    * still try to build SQL queries, although cannot guarantee
    * that it would be correct (ie, wrong dialect).
    */
    OTHER,
    
    MYSQL
  }
}