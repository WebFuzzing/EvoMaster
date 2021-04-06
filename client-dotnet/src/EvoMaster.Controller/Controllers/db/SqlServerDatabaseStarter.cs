using System;
using System.Data.Common;
using System.Threading.Tasks;
using Docker.DotNet;
using Microsoft.Data.SqlClient;

namespace EvoMaster.Controller.Controllers.db
{
    internal static class SqlServerDatabaseStarter
    {
        private static SqlServerContainer _sqlServerContainer;
        private static DockerClient _dockerClient;
        
        public static async Task<(string, DbConnection)> StartAsync(string databaseName, int port, int timeout)
        {
            var dockerUri = IsRunningOnWindows()
                ? "npipe://./pipe/docker_engine"
                : "unix:///var/run/docker.sock";

            _dockerClient = new DockerClientConfiguration(
                    // TODO: This needs to be configurable in order to execute tests in CI
                    new Uri(dockerUri))
                .CreateClient();

            DockerContainerBase.CleanupOrphanedContainersAsync(_dockerClient).Wait(timeout * 500);

            _sqlServerContainer = new SqlServerContainer(port);

            await _sqlServerContainer.StartAsync(_dockerClient, timeout);

            var connectionString = _sqlServerContainer.GetConnectionString(databaseName);

            //TODO: use logger
            Console.WriteLine($"*** SQL Server database started and the connection string is: \"{connectionString}\"");

            var connection = new SqlConnection(connectionString);

            return (connectionString, connection);
        }
        
        private static bool IsRunningOnWindows()
        {
            return Environment.OSVersion.Platform == PlatformID.Win32NT;
        }
    }
}