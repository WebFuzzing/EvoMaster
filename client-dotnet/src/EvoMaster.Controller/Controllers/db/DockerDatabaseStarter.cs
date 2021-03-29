using System.Data.Common;
using System.Threading.Tasks;
using EvoMaster.Controller.Api;

namespace EvoMaster.Controller.Controllers.db
{
    public static class DockerDatabaseStarter
    {
        public static async Task<(string, DbConnection)> StartAsync(DatabaseType databaseType, string databaseName,
            int timeout = 180)
        {
            return databaseType switch
            {
                DatabaseType.MSSQL => await MsSqlDockerStarter.StartDatabaseAsync(databaseName, timeout),
                DatabaseType.POSTGRES => await NpgsqlDockerStarter.StartDatabaseAsync(databaseName),
                _ => throw new DbUnsupportedException(databaseType)
            };
        }
    }
}