using System;
using System.Data.Common;
using System.Threading.Tasks;
using Docker.DotNet;
using EvoMaster.Controller.Api;
using Microsoft.Data.SqlClient;

namespace EvoMaster.Controller.Controllers.db
{
    //This class receives the request for starting a db container and passes it based on the db type to the proper class
    public static class DatabaseStarter
    {
        public static async Task<(string, DbConnection)> RunAsync(DatabaseType databaseType, string databaseName,
            int port,
            int timeout = 180)
        {
            return databaseType switch
            {
                DatabaseType.MS_SQL_SERVER => await SqlServerDatabaseStarter.StartAsync(databaseName, port, timeout),
                DatabaseType.POSTGRES => await PostgresDatabaseStarter.StartAsync(databaseName, port),
                _ => throw new DbUnsupportedException(databaseType)
            };
        }
    }
}